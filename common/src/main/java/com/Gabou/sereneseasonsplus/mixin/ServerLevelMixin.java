package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.LevelReader;
import sereneseasons.season.SeasonHooks;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private ServerChunkCache chunkSource;
    @Unique
    private static final int MIN_TICKS_INTERVALLES = 10;

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            ),
            cancellable = true
    )
    private void ssp$handleSnowAndIce(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel)(Object)this;
        ProfilerFiller profiler = level.getProfiler();

        // ✅ Run your seasonal snow queue logic first
        if (level.dimension() == Level.OVERWORLD) {
            int t = CommonSnowBlockFeature.getTickCounter();
            ChunkPos cpos = chunk.getPos();
            boolean doEval = ((cpos.x ^ cpos.z ^ t) & 15) == 0; // ~once per 16 ticks per ticking chunk
            if (doEval) {

            ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
            Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
            var seasonState = SeasonHelper.getSeasonState(level);

            if (seasonState != null && currentSeason != null) {
                // Lazily cache surface height if not set yet
                if (tracked.sereneseasonsplus$getSurfaceHeight() == -1) {
                    int surfaceHeightCache = level.getHeight(Heightmap.Types.WORLD_SURFACE, cpos.getMiddleBlockX(), cpos.getMiddleBlockZ());
                    tracked.sereneseasonsplus$setSurfaceHeight(surfaceHeightCache);
                }
                int surfaceHeight = tracked.sereneseasonsplus$getSurfaceHeight();
                SnowLogic.evaluate(level, currentSeason, seasonState, tracked, cpos, true, surfaceHeight);
            }
            }
        }

        // ✅ Replace vanilla ice/snow logic
        profiler.popPush("iceandsnow");
        if (level.random.nextInt(16) == 0) {
            int j = chunk.getPos().getMinBlockX();
            int k = chunk.getPos().getMinBlockZ();
            BlockPos blockPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(j, 0, k, 15));
            BlockPos blockPos2 = blockPos.below();
            Biome biome = level.getBiome(blockPos).value();
            // Freeze water only during cold precipitation (do not duplicate vanilla cold-freeze)
            if (EnvironmentHelper.isRainning(level, blockPos)
                    && CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, blockPos)) {
                CommonSnowBlockFeature.tryFreezeWaterAt(level, blockPos2);
            }

            // Snow accumulation (uses your helper instead of vanilla "bl")
            if (EnvironmentHelper.isRainning(level, blockPos)) {
                int maxSnow = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
                if (maxSnow > 0 && sereneseasons.season.SeasonHooks.shouldSnowHook(biome, level, blockPos)) {
                    BlockState state = level.getBlockState(blockPos);

                    if (state.is(Blocks.SNOW)) {
                        int layers = state.getValue(SnowLayerBlock.LAYERS);
                        if (layers < Math.min(maxSnow, 8)) {
                            BlockState next = state.setValue(SnowLayerBlock.LAYERS, layers + 1);
                            Block.pushEntitiesUp(state, next, level, blockPos);
                            if (level.setBlockAndUpdate(blockPos, next)) {
                                CommonSnowBlockFeature.accumulateColumnUpdate(blockPos, next);
                            }
                        }
                    } else {
                        BlockState snow = Blocks.SNOW.defaultBlockState();
                        if (level.setBlockAndUpdate(blockPos, snow)) {
                            CommonSnowBlockFeature.accumulateColumnUpdate(blockPos, snow);
                        }
                    }
                }

                // Precipitation hook
                Biome.Precipitation precipitation = biome.getPrecipitationAt(blockPos2);
                if (precipitation != Biome.Precipitation.NONE) {
                    BlockState base = level.getBlockState(blockPos2);
                    base.getBlock().handlePrecipitation(base, level, blockPos2, precipitation);
                }
            }
        }

        // Do not cancel the rest of tickChunk to avoid skipping other chunk work
    }

//    // Suppress vanilla snow/ice placement decisions inside tickChunk; our logic above replaces them
//    @Redirect(
//            method = "tickChunk",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
//            )
//    )
//    private boolean ssp$redirectShouldSnow(Biome biome, LevelReader levelReader, BlockPos pos) {
//        return false;
//    }
//
//    @Redirect(
//            method = "tickChunk",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
//            )
//    )
//    private boolean ssp$redirectShouldFreeze(Biome biome, LevelReader levelReader, BlockPos pos) {
//        return false;
//    }



}

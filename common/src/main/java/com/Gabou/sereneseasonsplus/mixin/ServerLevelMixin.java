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
        if (CommonSnowBlockFeature.getTickCounter() % MIN_TICKS_INTERVALLES == 0
                && level.dimension() == Level.OVERWORLD) {

            ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
            Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
            var seasonState = SeasonHelper.getSeasonState(level);

            if (seasonState != null && currentSeason != null) {
                int surfaceHeight = tracked.sereneseasonsplus$getSurfaceHeight();
                SnowLogic.evaluate(level, currentSeason, seasonState, tracked, chunk.getPos(), false, surfaceHeight);
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

            // Ice freeze (vanilla cold freeze or snow-precip freeze for water biomes)
            if (biome.shouldFreeze(level, blockPos2)) {
                BlockState ice = Blocks.ICE.defaultBlockState();
                if (level.setBlockAndUpdate(blockPos2, ice)) {
                    CommonSnowBlockFeature.accumulateColumnUpdate(blockPos2, ice);
                }
            } else if (EnvironmentHelper.isRainning(level, blockPos)
                    && CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, blockPos)) {
                CommonSnowBlockFeature.tryFreezeWaterAt(level, blockPos2);
            }

            // Snow accumulation (uses your helper instead of vanilla "bl")
            if (EnvironmentHelper.isRainning(level, blockPos)) {
                int maxSnow = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
                if (maxSnow > 0 && biome.shouldSnow(level, blockPos)) {
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

        // Cancel vanilla handling since we replaced it
        ci.cancel();
    }



}

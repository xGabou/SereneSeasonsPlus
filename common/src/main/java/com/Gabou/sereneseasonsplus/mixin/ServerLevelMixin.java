package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.access.IServerLevel;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.ServerPrecipitationService;
import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

@Mixin(value = ServerLevel.class)
public class ServerLevelMixin {

    @Unique
    private boolean sereneseasonsplus$shouldSkipSnowCheck = false;

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void ssp$resetAccumulators(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        sereneseasonsplus$shouldSkipSnowCheck = false;

        if (CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            int t = CommonSnowBlockFeature.getTickCounter();
            ChunkPos cpos = chunk.getPos();
            boolean doEval = ((cpos.x ^ cpos.z ^ t) & 15) == 0; // ~once per 16 ticks per ticking chunk
            if (doEval && chunk instanceof ISnowTrackedChunk tracked) {
                Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
                var seasonState = SeasonHelper.getSeasonState(level);

                if (seasonState != null && currentSeason != null) {
                    if (tracked.sereneseasonsplus$getSurfaceHeight() == -1) {
                        int surfaceHeightCache = level.getHeight(
                                Heightmap.Types.WORLD_SURFACE,
                                cpos.getMiddleBlockX(),
                                cpos.getMiddleBlockZ()
                        );
                        tracked.sereneseasonsplus$setSurfaceHeight(surfaceHeightCache);
                    }

                    int surfaceHeight = tracked.sereneseasonsplus$getSurfaceHeight();
                    SnowLogic.evaluate(level, currentSeason, seasonState, tracked, cpos, true, surfaceHeight);
                }
            }
        }
    }

    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            )
    )
    private boolean ssp$AccumulateColumnUpdate(ServerLevel level, BlockPos pos, BlockState state) {
        return ServerPrecipitationService.setBlockAndTrackSnow(level, pos, state);
    }

    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean sereneseasonsplus$redirectShouldFreeze(Biome biome, LevelReader level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel
                && CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(serverLevel, pos)) {
            CommonSnowBlockFeature.tryFreezeWaterAt(serverLevel, pos);
        }
        return false;
    }

    @Inject(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
                    )
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void beforeSnowCheck(
            BlockPos $$0,
            CallbackInfo ci,
            BlockPos posSnowCheck,
            BlockPos posBelow,
            Biome biome,
            int snowHeight,
            BlockState state16
    ) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            sereneseasonsplus$shouldSkipSnowCheck = false;
            return;
        }

        if ((Object) this instanceof IServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.sereneseasonsplus$getChunkLevel();
            sereneseasonsplus$shouldSkipSnowCheck =
                    chunk != null && ServerPrecipitationService.isDestroyedDuringCurrentStorm(chunk, posSnowCheck);
        } else {
            sereneseasonsplus$shouldSkipSnowCheck = false;
        }
    }

    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
                    )
            ),
            require = 1
    )
    private boolean sereneseasonsplus$gateSnowIsCheckSliced(BlockState state, Block block) {
        return ServerPrecipitationService.shouldTreatAsSnow(state, block, sereneseasonsplus$shouldSkipSnowCheck);
    }

    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
                    )
            )
    )
    private boolean sereneseasonsplus$redirectFreshSnowOnly(ServerLevel level, BlockPos pos, BlockState state) {
        if (sereneseasonsplus$shouldSkipSnowCheck) {
            return false;
        }

        if (!ServerPrecipitationService.canPlaceSnowWithoutReplacingImportant(level, pos, state)) {
            return false;
        }
        return ServerPrecipitationService.setBlockAndTrackSnow(level, pos, state);
    }
}

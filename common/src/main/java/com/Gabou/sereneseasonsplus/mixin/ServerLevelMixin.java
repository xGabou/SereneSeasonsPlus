package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.SereneSeasonPlusCommon;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.logic.SnowChunkWeatherLogic;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ServerLevel.class)
public class ServerLevelMixin {


    @Shadow
    @Final
    private static Logger LOGGER;
    @Unique
    private boolean sereneseasonsplus$isRaining;


    @Unique
    private BlockPos atmosphere$freezePos;
    @Unique
    private ServerLevel atmosphere$level;

    @Unique
    private boolean sereneseasonsplus$shouldSkipSnowCheck = false;

    @Unique
    private LevelChunk sereneseasonsplus$currentPrecipitationChunk;

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tickPrecipitation(Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void sereneseasonsplus$capturePrecipChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        this.sereneseasonsplus$currentPrecipitationChunk = chunk;
    }

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tickPrecipitation(Lnet/minecraft/core/BlockPos;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void sereneseasonsplus$clearPrecipChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        this.sereneseasonsplus$currentPrecipitationChunk = null;
    }


    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void ssp$resetAccumulators(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        sereneseasonsplus$isRaining = EnvironmentHelper.isRainning(level, chunk.getPos().getMiddleBlockPosition(63));
        sereneseasonsplus$shouldSkipSnowCheck = false;

    }


    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void ssp$handleSnowAndIce(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) return;
        ServerLevel level = (ServerLevel) (Object) this;
        SnowChunkWeatherLogic.run(level, chunk);
    }


    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0   // <- snow layer-increase update
            )
    )
    private boolean ssp$AccumulateColumnUpdate(ServerLevel level, BlockPos pos, BlockState state) {
        boolean result = level.setBlockAndUpdate(pos, state);
        if (result) {
            CommonSnowBlockFeature.accumulateColumnUpdate(level, pos, state);
        }
        return result;
    }

    @Inject(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void atmosphere$captureFreezeContext(
            BlockPos pos,
            CallbackInfo ci,
            BlockPos heightmapPos,
            BlockPos posBelow,
            Biome biome
    ) {
        this.atmosphere$freezePos = posBelow;
        this.atmosphere$level = (ServerLevel) (Object) this;
    }


    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean atmosphere$interceptShouldFreeze(
            Biome biome,
            LevelReader levelReader,
            BlockPos posBelow
    ) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            return biome.shouldFreeze(levelReader, posBelow);
        }

        ServerLevel level = (ServerLevel) (Object) this;

        if (CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, posBelow)) {
            CommonSnowBlockFeature.tryFreezeWaterAt(level, posBelow);
        }

        return false;
    }


    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;isRaining()Z"
            )
    )
    private boolean overrideIsRaining(ServerLevel level) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            return level.isRaining();
        }
        if (level.dimension() == Level.OVERWORLD) {
            return sereneseasonsplus$isRaining;
        }
        return level.isRaining();
    }


    @Inject(
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
                            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
                    )
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void beforeSnowCheck(BlockPos $$0, CallbackInfo ci, BlockPos posSnowCheck, BlockPos posBelow, Biome biome, int snowHeight, BlockState $$5) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            sereneseasonsplus$shouldSkipSnowCheck = false;
            return;
        }
        if(sereneseasonsplus$currentPrecipitationChunk == null) {
            LOGGER.info("Current precipitation chunk is null during snow check at {}, skipping Sereneseasons+ snow logic to avoid potential issues.", posSnowCheck);
            return;
        }
        sereneseasonsplus$shouldSkipSnowCheck = false;
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
                            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
                    )
            )
    )
    private boolean sereneseasonsplus$skipSnowBlockCheck(BlockState state, Block block) {
        return !sereneseasonsplus$shouldSkipSnowCheck && state.is(block);
    }


    @Redirect(
            method = "tickPrecipitation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1   // ← ONLY the `else { setBlockAndUpdate($$12, Blocks.SNOW...) }`
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"
                    )
            )
    )
    private boolean sereneseasonsplus$redirectFreshSnowOnly(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            return level.setBlockAndUpdate(pos, state);
        }
        if (sereneseasonsplus$shouldSkipSnowCheck) {
            // Skip ONLY the fresh snow placement in the `else` branch
            return false;
        }

        // Let vanilla handle it when not skipping
        boolean result = level.setBlockAndUpdate(pos, state);
        if (result) {
            CommonSnowBlockFeature.accumulateColumnUpdate(level, pos, state);
        }
        return result;
    }


}

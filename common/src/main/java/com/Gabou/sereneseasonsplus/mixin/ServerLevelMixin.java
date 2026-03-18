package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.logic.SnowChunkWeatherLogic;
import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonHooks;

@Mixin(value = ServerLevel.class)
public class ServerLevelMixin {



    @Unique
    private boolean sereneseasonsplus$isRaining;


    @Unique
    private BlockPos atmosphere$freezePos;
    @Unique
    private ServerLevel atmosphere$level;

    @Unique
    private boolean sereneseasonsplus$shouldSkipSnowCheck = false;


    @Inject(method = "tickChunk",at = @At("HEAD"))
    private void ssp$resetAccumulators(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel)(Object)this;
        sereneseasonsplus$isRaining = EnvironmentHelper.isRainning(level, chunk.getPos().getMiddleBlockPosition(63));
        sereneseasonsplus$shouldSkipSnowCheck = false;

    }

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getBlockRandomPos(I I I I)Lnet/minecraft/core/BlockPos;",
                    shift = At.Shift.BEFORE,
                    ordinal = 1
            )

    )
    private void beforeRandomHeightmapPos(
            LevelChunk chunk,
            int randomTickSpeed,
            CallbackInfo ci
    ) {
        // Chunk evaluation is handled in SnowChunkWeatherLogic.run.
        // Keeping this inject empty avoids scheduling the same work twice per tickChunk.
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
            method = "tickChunk",
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
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void captureLocals(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci,
                               ChunkPos chunkPos, boolean precipitationFlag,
                               int minBlockX, int minBlockZ,
                               ProfilerFiller profiler,
                               BlockPos posSnowCheck,   // $$12
                               BlockPos posBelow,       // $$13
                               Biome biome) {           // $$14

        atmosphere$freezePos = posBelow;
        atmosphere$level = (ServerLevel)(Object)this;
    }


    @ModifyVariable(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
                    shift = At.Shift.AFTER
            ),
            ordinal = 0
    )
    private boolean interceptShouldFreeze(boolean original) {
        if (!CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            return original;
        }

        if (CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(atmosphere$level, atmosphere$freezePos)) {
            CommonSnowBlockFeature.tryFreezeWaterAt(atmosphere$level, atmosphere$freezePos);
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
            method = "tickChunk",
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
    private void beforeSnowCheck(
            LevelChunk chunk,
            int randomTickSpeed,
            CallbackInfo ci,
            ChunkPos chunkPos,
            boolean isRaining,
            int minBlockX,
            int minBlockZ,
            ProfilerFiller profiler,
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
        boolean skip = false;

        SnowHistorySavedData sd = SnowHistorySavedData.get();
        int activeId = sd != null ? sd.currentStormId : 0;

        if (activeId > 0 && chunk instanceof ISnowTrackedChunk tracked) {
            if (tracked.sereneseasonsplus$getDestroyedStormId() != activeId) {
                tracked.sereneseasonsplus$getDestroyedColumns().clear();
                tracked.sereneseasonsplus$setDestroyedStormId(activeId);
            }
            long xz = (((long) posSnowCheck.getX()) << 32) ^ (posSnowCheck.getZ() & 0xFFFFFFFFL);
            skip = tracked.sereneseasonsplus$getDestroyedColumns().contains(xz);
        }

        sereneseasonsplus$shouldSkipSnowCheck = skip;
    }

    @ModifyVariable(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    shift = At.Shift.AFTER
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
            ordinal = 0
    )
    private boolean skipSnowIf(boolean original) {
        return !sereneseasonsplus$shouldSkipSnowCheck && original;
    }







    @Redirect(
            method = "tickChunk",
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

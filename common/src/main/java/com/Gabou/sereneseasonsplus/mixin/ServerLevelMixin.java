package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Shadow
    @Final
    private static Logger LOGGER;
    @Unique
    private static final int MIN_TICKS_INTERVALLES = 10;

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void snow$addToQueue(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        // Rate limit how often we consider (reduces queue churn)
        if (CommonSnowBlockFeature.getTickCounter() % MIN_TICKS_INTERVALLES != 0) return;

        if (!(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) return;

        ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;

        if(chunk.getPos().equals(new ChunkPos(7,50)))
        {
            LOGGER.info("Ticking chunk at pos: {} in season: {}", chunk.getPos(), SeasonHelper.getSeasonState(level).getSubSeason());
        }

        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();


        if (tracked.sereneseasonsplus$getLastSeason() != currentSeason) {
            tracked.sereneseasonsplus$setLastSeason(currentSeason);

        }

        boolean wasRaining = tracked.sereneseasonsplus$wasRaining();
        boolean isRaining = EnvironmentHelper.isRainning(level, chunk.getPos().getMiddleBlockPosition(65));
        if (isRaining != wasRaining) {
            CommonSnowBlockFeature.HANDLER.onRainChanged(level, chunk.getPos(), isRaining);
            tracked.sereneseasonsplus$incrementWasRaining(isRaining);
            if (!isRaining) {
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);
            }
        }

        if (EnvironmentHelper.isSnowySeason()) {
            // Virgin or reset chunk, not yet snowed this storm
            if(tracked.sereneseasonsplus$getLastWinterId() != EnvironmentHelper.getCurrentWinterId())
            {
                tracked.sereneseasonsplus$setLastWinterId(EnvironmentHelper.getCurrentWinterId());
                tracked.sereneseasonsplus$setSnowCount(-1);
                tracked.sereneseasonsplus$setHasAppliedInitialSnow(false);
                tracked.sereneseasonsplus$setShouldApplyInitialSnow(false);
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);
                tracked.sereneseasonsplus$willReceiveSnow(false);
                return;
            }
            boolean shouldSnow = CommonSnowBlockFeature.HANDLER.shouldApplySnow(level, chunk.getPos());
            if (shouldSnow && tracked.sereneseasonsplus$getSnowCount() <= 0
                    && !tracked.sereneseasonsplus$hasReceivedSnowLayerThisStorm()) {

                ChunkQueue.enqueueApply(chunk.getPos(), currentSeason);
                tracked.sereneseasonsplus$willReceiveSnow(true);
                return;
            }

            // Already flagged to receive → retry until success
            if (tracked.sereneseasonsplus$shouldReceiveSnow()) {
                ChunkQueue.enqueueApply(chunk.getPos(), currentSeason);
                return;
            }
        }
        else if (EnvironmentHelper.isHotSeason() || tracked.sereneseasonsplus$getSnowCount() <= 0 ) {
            if (tracked.sereneseasonsplus$getSnowCount() > 0 || tracked.sereneseasonsplus$getSnowCount() == -1) {
                ChunkQueue.enqueueMelt(chunk.getPos(), true);
                tracked.sereneseasonsplus$setSnowCount(0);
                return;
            }
        }

        //if(tracked.sereneseasonsplus$getSnowCount() <= 0)
            //LOGGER.info("Ticking chunk at pos: {}", chunk.getPos());






    }

}

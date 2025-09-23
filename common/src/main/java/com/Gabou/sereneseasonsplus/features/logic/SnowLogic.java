package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;


/**
 * Central snow evaluation logic.
 * Called from both chunk load (bootstrap) and periodic tick (maintenance).
 */
public final class SnowLogic {

    private SnowLogic() {}

    public static void evaluate(ServerLevel level,
                                Season.SubSeason currentSeason,
                                ISeasonState seasonState,
                                ISnowTrackedChunk tracked,
                                ChunkPos chunkPos,
                                boolean isLoadEvent) {

        // Track season transitions
        Season.SubSeason prevSeason = tracked.sereneseasonsplus$getLastSeason();
        if (prevSeason != currentSeason) {
            tracked.sereneseasonsplus$setLastSeason(currentSeason);
        }

        // Detect rainfall change
        boolean wasRaining = tracked.sereneseasonsplus$wasRaining();
        boolean isRaining = EnvironmentHelper.isRainning(level, chunkPos.getMiddleBlockPosition(65));
        if (isRaining != wasRaining) {
            CommonSnowBlockFeature.HANDLER.onRainChanged(level, chunkPos, isRaining);
            tracked.sereneseasonsplus$incrementWasRaining(isRaining);
            if (!isRaining) {
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);
            }
        }

        // Reset per winter
        int globalWinterId = EnvironmentHelper.getCurrentWinterId();
        if (tracked.sereneseasonsplus$getLastWinterId() != globalWinterId) {
            tracked.sereneseasonsplus$setLastWinterId(globalWinterId);
            tracked.sereneseasonsplus$setSnowCount(-1);
            tracked.sereneseasonsplus$setHasAppliedInitialSnow(false);
            tracked.sereneseasonsplus$setShouldApplyInitialSnow(false);
            tracked.sereneseasonsplus$willReceiveSnow(false);
            tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);

            // New requirement: at the start of a new winter, if this chunk
            // has not yet received snowfall for the current winter, clear any
            // stale snow it might still have from previous seasons.
            boolean seenThisWinter = CommonSnowBlockFeature.HANDLER.hasChunkSeenSnow(level, chunkPos)
                    || tracked.sereneseasonsplus$getSnowCount() > 0;
            if (!seenThisWinter) {
                ChunkQueue.enqueueMelt(chunkPos, true);
            }

            // On tick events, bail here so next cycle applies snow
            if (!isLoadEvent) return;
        }

        // Seasonal snow vs melt
        CommonSnowBlockFeature.LayerBounds bounds = CommonSnowBlockFeature.getSeasonalLayerBounds(currentSeason, seasonState.getDay());
        if (bounds != null) { // snowy
            boolean pendingSnow = CommonSnowBlockFeature.HANDLER.shouldApplySnow(level, chunkPos);
            boolean hasSnowHistory = pendingSnow
                    || CommonSnowBlockFeature.HANDLER.hasChunkSeenSnow(level, chunkPos)
                    || tracked.sereneseasonsplus$getSnowCount() > 0;

            boolean needsInitial = !tracked.sereneseasonsplus$hasAppliedInitialSnow()
                    || tracked.sereneseasonsplus$getSnowCount() <= 0;

            if (pendingSnow || (needsInitial && hasSnowHistory) || tracked.sereneseasonsplus$shouldReceiveSnow()) {
                ChunkQueue.enqueueApply(chunkPos, currentSeason);
                tracked.sereneseasonsplus$willReceiveSnow(true);
            }
        } else { // non-snowy
            boolean longGap = prevSeason == null
                    || Math.abs(currentSeason.ordinal() - prevSeason.ordinal()) != 1
                    || EnvironmentHelper.isHotSeason();

            ChunkQueue.enqueueMelt(chunkPos, longGap);
        }
    }
}

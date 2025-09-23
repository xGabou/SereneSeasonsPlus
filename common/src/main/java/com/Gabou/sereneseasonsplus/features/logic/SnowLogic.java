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

        Season.SubSeason prevSeason = tracked.sereneseasonsplus$getLastSeason();
        if (prevSeason != currentSeason) {
            tracked.sereneseasonsplus$setLastSeason(currentSeason);
        }

        boolean wasRaining = tracked.sereneseasonsplus$wasRaining();
        boolean isRaining = EnvironmentHelper.isRainning(level, chunkPos.getMiddleBlockPosition(65));
        if (isRaining != wasRaining) {
            CommonSnowBlockFeature.HANDLER.onRainChanged(level, chunkPos, isRaining);
            tracked.sereneseasonsplus$incrementWasRaining(isRaining);

            // storm just ended on this chunk
            if (!isRaining) {
                // if this chunk actually received a layer during that storm
                // we can consider the first storm for this winter completed globally
                if (tracked.sereneseasonsplus$hasReceivedSnowLayerThisStorm()) {
                    WinterFlags.markFirstStormFinished(level, EnvironmentHelper.getCurrentWinterId());
                }
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);
            }
        }

        int globalWinterId = EnvironmentHelper.getCurrentWinterId();
        if (tracked.sereneseasonsplus$getLastWinterId() != globalWinterId) {
            tracked.sereneseasonsplus$setLastWinterId(globalWinterId);
            tracked.sereneseasonsplus$setSnowCount(-1);
            tracked.sereneseasonsplus$setHasAppliedInitialSnow(false);
            tracked.sereneseasonsplus$setShouldApplyInitialSnow(false);
            tracked.sereneseasonsplus$willReceiveSnow(false);
            tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);

            // clear stale snow if this chunk has not actually seen snow in this new winter
            boolean seenThisWinter = CommonSnowBlockFeature.HANDLER.hasChunkSeenSnow(level, chunkPos)
                    || tracked.sereneseasonsplus$getSnowCount() > 0;
            if (!seenThisWinter) {
                ChunkQueue.enqueueMelt(chunkPos, true);
            }

            if (!isLoadEvent) return;
        }

        CommonSnowBlockFeature.LayerBounds bounds =
                CommonSnowBlockFeature.getSeasonalLayerBounds(currentSeason, seasonState.getDay());

        if (bounds != null) {
            boolean pendingSnow = CommonSnowBlockFeature.HANDLER.shouldApplySnow(level, chunkPos);
            boolean hasSnowHistory = pendingSnow
                    || CommonSnowBlockFeature.HANDLER.hasChunkSeenSnow(level, chunkPos)
                    || tracked.sereneseasonsplus$getSnowCount() > 0;

            // mark that this chunk received a layer during the ongoing storm once we decide to apply
            if (pendingSnow) {
                tracked.sereneseasonsplus$setShouldApplyInitialSnow(true);
            }

            int sc = tracked.sereneseasonsplus$getSnowCount();
            boolean firstTimeHere = sc <= 0;

            // initial spread should wait until the first storm of the winter has finished
            boolean firstStormFinished = WinterFlags.hasFirstStormFinished(level, globalWinterId);
            boolean okForInitialSpread = firstStormFinished && !isRaining;

            boolean needsInitial = firstTimeHere && !tracked.sereneseasonsplus$hasAppliedInitialSnow();

            // Three possible reasons to apply
            // 1 natural storm snowfall now for this chunk
            // 2 post storm initial spread for chunks that should be snowy but missed the storm
            // 3 previously flagged retry
            boolean applyNow =
                    (pendingSnow && firstTimeHere)
                            || (okForInitialSpread && needsInitial && hasSnowHistory)
                            || tracked.sereneseasonsplus$shouldReceiveSnow();

            if (applyNow) {
                ChunkQueue.enqueueApply(chunkPos, currentSeason);
                tracked.sereneseasonsplus$willReceiveSnow(true);

                if (pendingSnow) {
                    // this chunk actually participated in the storm
                    tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);
                } else if (okForInitialSpread && needsInitial) {
                    // mark that this chunk has consumed its initial spread opportunity
                    tracked.sereneseasonsplus$setHasAppliedInitialSnow(true);
                }
            }

            // do not force clear shouldApplyInitialSnow here
            // let the queue success path clear your flags where you already do so
        } else {
            boolean longGap = prevSeason == null
                    || Math.abs(currentSeason.ordinal() - prevSeason.ordinal()) != 1
                    || EnvironmentHelper.isHotSeason();

            ChunkQueue.enqueueMelt(chunkPos, longGap);
        }
    }

}

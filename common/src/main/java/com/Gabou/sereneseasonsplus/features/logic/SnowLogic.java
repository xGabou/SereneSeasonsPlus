package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;

/**
 * Central snow evaluation logic.
 * Now driven by coldness and global snow history.
 */
public final class SnowLogic {

    private SnowLogic() {}

    public static void evaluate(ServerLevel level,
                                Season.SubSeason currentSeason,
                                ISeasonState seasonState,
                                ISnowTrackedChunk tracked,
                                ChunkPos chunkPos,
                                boolean isLoadEvent,
                                int maxHeight) {
        if (chunkPos.equals(new ChunkPos(-328, 355))) {
            CommonSnowBlockFeature.LOGGER.info("test");
        }

        // --- Check temperature ---
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(Math.max(level.getMinBuildHeight(), maxHeight));
        boolean coldEnough = CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, samplePos);
        boolean snowingNow = EnvironmentHelper.isRainning(level, samplePos);
        boolean allowApply = snowingNow || isLoadEvent;

        // --- Case 1: Cold enough => pile snow ---
        if (coldEnough) {
            float globalAvg = CommonSnowBlockFeature.computeGlobalAvg(level);
            int totalPositions = tracked.sereneseasonsplus$getTrackedColumnCount();

            // Ensure per-column baseline from finished storms is met across the chunk
            int baseline = CommonSnowBlockFeature.computeGlobalMinSum(level);
            if (baseline > 0) {
                int baselineTotal = baseline * 256; // 16x16 columns per chunk
                int trackedTotal = tracked.sereneseasonsplus$getTotalSnowLayers();
                if (trackedTotal < baselineTotal && allowApply) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                    return;
                }
            }

            if (totalPositions == 0) {
                if (globalAvg > 0.5f && allowApply) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                }
            } else {
                float currentAvg = (float) tracked.sereneseasonsplus$getTotalSnowLayers() / (float) totalPositions;
                if (Math.abs(currentAvg - globalAvg) > 0.5f && allowApply) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                }
            }

            // If a storm is currently active, drive a random piling pass using the active storm record
            com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData sd = com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData.get(level);
            if (sd != null && sd.currentStormId > 0) {
                ChunkQueue.enqueueApply(chunkPos, currentSeason);
            }

        }
        // --- Case 2: Too warm => melt snow ---
        else {
            // Unconditional melt in warm sub-seasons
            boolean inWarmSeason = currentSeason.ordinal() >= Season.SubSeason.LATE_SPRING.ordinal()
                    && currentSeason.ordinal() < Season.SubSeason.EARLY_WINTER.ordinal();

            // Conditional melt only at start of winter if no storms occurred
            boolean inEarlyWinterNoStorm = currentSeason == Season.SubSeason.EARLY_WINTER
                    && CommonSnowBlockFeature.HANDLER.getSnowStormsThisWinter(level) == 0;

            if (inWarmSeason || inEarlyWinterNoStorm) {
                // melt all tracked snow columns until ground
                ChunkQueue.enqueueMelt(chunkPos, false);
            }
        }
    }
}


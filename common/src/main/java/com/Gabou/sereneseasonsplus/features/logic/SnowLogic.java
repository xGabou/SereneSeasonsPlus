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
//        if (chunkPos.equals(new ChunkPos(-6, -8))) {
//            CommonSnowBlockFeature.LOGGER.info("test");
//        }

        // --- Check temperature ---
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(Math.max(level.getMinBuildHeight(), maxHeight));
        boolean coldEnough = CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, samplePos);
        boolean snowingNow = EnvironmentHelper.isRainning(level, samplePos);
        boolean allowApply = snowingNow || isLoadEvent;

        // --- Case 1: Cold enough => pile snow ---
        if (coldEnough) {
            float globalAvg = CommonSnowBlockFeature.computeGlobalAvg(level);
            int totalPositions = tracked.sereneseasonsplus$getTrackedColumnCount();

            // 1️⃣ Global baseline balance (macro-scale)
            int baseline = CommonSnowBlockFeature.computeGlobalMinSum(level);
            if (baseline > 0) {
                int estimatedCols = tracked.sereneseasonsplus$getAvailableSnowColumns();
                if (estimatedCols <= 0) estimatedCols = 256; // fallback if not yet computed
                int baselineTotal = baseline * estimatedCols; // scale to real available columns
                int trackedTotal = tracked.sereneseasonsplus$getTotalSnowLayers();

                // Only react if the chunk differs by ±25 % or more from baseline
                float ratio = trackedTotal / (float) baselineTotal;
                if ((ratio < 0.75f) && allowApply) {
//                    if (chunkPos.equals(new ChunkPos(-5, -8))) {
//                        CommonSnowBlockFeature.LOGGER.info("test");
//                    }
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                    return;
                }
            }

            // 2️⃣ Per-column average stability (micro-scale)
            if (totalPositions == 0) {
                if (globalAvg > 0.5f && allowApply) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                }
            } else {
                float currentAvg = tracked.sereneseasonsplus$getTotalSnowLayers() / (float) totalPositions;

                // Compute proportional tolerance (e.g. ±20 % of global average, minimum 2 layers)
                float tolerance = Math.max(2.0f, globalAvg * 0.20f);

                if ((globalAvg - currentAvg) > tolerance && allowApply) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                }

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

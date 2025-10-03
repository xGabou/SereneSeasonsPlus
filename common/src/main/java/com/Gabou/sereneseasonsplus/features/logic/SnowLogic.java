package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.SereneSeasonPlusCommon;
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
        if(chunkPos.equals(new ChunkPos(-328,355))){
            CommonSnowBlockFeature.LOGGER.info("test");
        }

        // --- Check temperature ---
        boolean coldEnough = CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, chunkPos.getMiddleBlockPosition(maxHeight));


        // --- Case 1: Cold enough => pile snow ---
        if (coldEnough) {
            float globalAvg = CommonSnowBlockFeature.computeGlobalAvg(level);
            int totalPositions = tracked.sereneseasonsplus$getTrackedColumnCount();

            // Ensure per-column baseline from finished storms is met across the chunk
            int baseline = CommonSnowBlockFeature.computeGlobalMinSum(level);
            if (baseline > 0) {
                int baselineTotal = baseline * 256; // 16x16 columns per chunk
                int trackedTotal = tracked.sereneseasonsplus$getTotalSnowLayers();
                if (trackedTotal < baselineTotal) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                    return;
                }
            }

            if (totalPositions == 0) {
                if (globalAvg > 0.5f) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                }
            } else {
                float currentAvg = (float) tracked.sereneseasonsplus$getTotalSnowLayers() / (float) totalPositions;
                if (Math.abs(currentAvg - globalAvg) > 0.5f) {
                    ChunkQueue.enqueueApply(chunkPos, currentSeason);
                }
            }
        }

        // --- Case 2: Too warm => melt snow ---
        else {
            // Fonte inconditionnelle en saison chaude
            boolean inWarmSeason = currentSeason.ordinal() >= Season.SubSeason.LATE_SPRING.ordinal()
                    && currentSeason.ordinal() < Season.SubSeason.EARLY_WINTER.ordinal();

            // Fonte conditionnelle seulement en début d’hiver (si aucune tempête encore)
            boolean inEarlyWinterNoStorm = currentSeason == Season.SubSeason.EARLY_WINTER
                    && CommonSnowBlockFeature.HANDLER.getSnowStormsThisWinter(level) == 0;

            if (inWarmSeason || inEarlyWinterNoStorm) {
                // melt all tracked snow columns until ground
                ChunkQueue.enqueueMelt(chunkPos, false);
            }
        }


    }
}

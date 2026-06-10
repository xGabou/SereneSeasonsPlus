package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.features.SnowHistoryQueryService;
import com.Gabou.sereneseasonsplus.features.SnowStateService;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.Season;

public final class SnowAccumulationPolicy {
    public enum Action {
        NONE,
        APPLY,
        MELT
    }

    public enum Reason {
        NONE,
        LOAD_RESTORE_TRACKED,
        BASELINE_DEFICIT,
        EMPTY_CHUNK_HISTORY,
        AVERAGE_DEFICIT,
        WARM_SEASON_MELT,
        EARLY_WINTER_NO_STORM_MELT
    }

    public record ChunkDecision(Action action, boolean fullClear, Reason reason) {
        public static ChunkDecision none() {
            return new ChunkDecision(Action.NONE, false, Reason.NONE);
        }
    }

    private final SnowHistoryQueryService historyQueryService;
    private final SnowStateService stateService;

    public SnowAccumulationPolicy(SnowHistoryQueryService historyQueryService, SnowStateService stateService) {
        this.historyQueryService = historyQueryService;
        this.stateService = stateService;
    }

    public ChunkDecision evaluateChunk(ServerLevel level,
                                       Season.SubSeason currentSeason,
                                       ISnowTrackedChunk tracked,
                                       ChunkPos chunkPos,
                                       boolean isLoadEvent,
                                       int sampleHeight,
                                       boolean coldEnoughOverride) {
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(Math.max(level.getMinBuildHeight(), sampleHeight));
        boolean snowingNow = EnvironmentHelper.isRainning(level, samplePos);
        boolean allowApply = snowingNow || isLoadEvent;

        if (coldEnoughOverride) {
            if (isLoadEvent && stateService.hasTrackedSnow(tracked)) {
                return new ChunkDecision(Action.APPLY, false, Reason.LOAD_RESTORE_TRACKED);
            }

            int trackedTotal = tracked.sereneseasonsplus$getTotalSnowLayers();
            int baseline = historyQueryService.computeGlobalMinSum(level);
            if (baseline > 0) {
                int estimatedColumns = stateService.getAvailableSnowColumnsOrDefault(tracked, 256);
                int baselineTotal = baseline * estimatedColumns;
                if (baselineTotal > 0) {
                    float ratio = trackedTotal / (float) baselineTotal;
                    if (ratio < 0.75f && allowApply) {
                        return new ChunkDecision(Action.APPLY, false, Reason.BASELINE_DEFICIT);
                    }
                }
            }

            int trackedPositions = tracked.sereneseasonsplus$getTrackedColumnCount();
            float globalAverage = historyQueryService.computeGlobalAvg(level);
            if (trackedPositions == 0) {
                if (globalAverage > 0.5f && allowApply) {
                    return new ChunkDecision(Action.APPLY, false, Reason.EMPTY_CHUNK_HISTORY);
                }
            } else {
                float currentAverage = trackedTotal / (float) trackedPositions;
                float tolerance = Math.max(2.0f, globalAverage * 0.20f);
                if ((globalAverage - currentAverage) > tolerance && allowApply) {
                    return new ChunkDecision(Action.APPLY, false, Reason.AVERAGE_DEFICIT);
                }
            }
            return ChunkDecision.none();
        }

        boolean inWarmSeason = currentSeason.ordinal() >= Season.SubSeason.LATE_SPRING.ordinal()
                && currentSeason.ordinal() < Season.SubSeason.EARLY_WINTER.ordinal();
        if (inWarmSeason) {
            return new ChunkDecision(Action.MELT, false, Reason.WARM_SEASON_MELT);
        }

        boolean inEarlyWinterNoStorm = currentSeason == Season.SubSeason.EARLY_WINTER
                && com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature.HANDLER.getSnowStormsThisWinter(level) == 0;
        if (inEarlyWinterNoStorm) {
            return new ChunkDecision(Action.MELT, false, Reason.EARLY_WINTER_NO_STORM_MELT);
        }

        return ChunkDecision.none();
    }
}

package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.features.SnowHistoryQueryService;
import com.Gabou.sereneseasonsplus.features.SnowStateService;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
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
        SNOW_SYNC_GENERATION_CHANGED,
        STORM_COUNT_CHANGED,
        ACTIVE_STORM_PROGRESS,
        BASELINE_DEFICIT,
        EMPTY_CHUNK_HISTORY,
        AVERAGE_DEFICIT,
        WARM_SEASON_MELT,
        LOCAL_TEMPERATURE_MELT,
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
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(Math.max(level.getMinY(), sampleHeight));
        boolean snowingNow = EnvironmentHelper.isRainning(level, samplePos);
        boolean allowApply = snowingNow || isLoadEvent;

        boolean inWarmSeason = currentSeason.ordinal() >= Season.SubSeason.LATE_SPRING.ordinal()
                && currentSeason.ordinal() < Season.SubSeason.EARLY_WINTER.ordinal();
        if (inWarmSeason) {
            return new ChunkDecision(Action.MELT, true, Reason.WARM_SEASON_MELT);
        }

        boolean inEarlyWinterNoStorm = currentSeason == Season.SubSeason.EARLY_WINTER
                && CommonSnowBlockFeature.HANDLER.getSnowStormsThisWinter(level) == 0;
        if (inEarlyWinterNoStorm) {
            return new ChunkDecision(Action.MELT, true, Reason.EARLY_WINTER_NO_STORM_MELT);
        }

        if (!coldEnoughOverride) {
            return new ChunkDecision(Action.MELT, true, Reason.LOCAL_TEMPERATURE_MELT);
        }

        if (coldEnoughOverride) {
            SnowHistorySavedData savedData = SnowHistorySavedData.get();
            int activeStormId = savedData != null ? savedData.currentStormId : 0;
            boolean activeStormNeedsProgress = activeStormId > 0
                    && allowApply
                    && (tracked.sereneseasonsplus$getStormIdApplied() != activeStormId
                    || tracked.sereneseasonsplus$getStormProgress() < 1.0f);
            if (activeStormNeedsProgress) {
                return new ChunkDecision(Action.APPLY, false, Reason.ACTIVE_STORM_PROGRESS);
            }

            boolean snowSyncRequired = tracked.sereneseasonsplus$getSnowSyncGeneration()
                    != CommonSnowBlockFeature.getSnowSyncGeneration();
            if (snowSyncRequired && savedData != null && !savedData.snowHistory.isEmpty()) {
                return new ChunkDecision(Action.APPLY, false, Reason.SNOW_SYNC_GENERATION_CHANGED);
            }

            int serverStormCount = CommonSnowBlockFeature.HANDLER.getSnowStormsThisWinter(level);
            if (serverStormCount > tracked.sereneseasonsplus$getAppliedStormCount()) {
                return new ChunkDecision(Action.APPLY, false, Reason.STORM_COUNT_CHANGED);
            }
            return ChunkDecision.none();
        }

        return ChunkDecision.none();
    }
}

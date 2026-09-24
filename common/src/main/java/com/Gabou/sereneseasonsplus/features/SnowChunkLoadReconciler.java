package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.features.logic.SnowAccumulationPolicy;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

/**
 * Reconciles a chunk while its load callback is still running. Keeping this work
 * chunk-local makes the chunk packet contain the correct snow state without a
 * render-distance scan or a multi-tick visible catch-up.
 */
public final class SnowChunkLoadReconciler {
    private final SnowStateService stateService;

    public SnowChunkLoadReconciler(SnowStateService stateService) {
        this.stateService = stateService;
    }

    public boolean reconcile(ServerLevel level, LevelChunk chunk) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) {
            return false;
        }

        initializeChunkMetadata(level, chunk, tracked);

        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
        var seasonState = SeasonHelper.getSeasonState(level);
        if (currentSeason == null || seasonState == null) {
            return false;
        }

        ChunkPos chunkPos = chunk.getPos();
        int sampleY = Math.max(level.getMinY(), tracked.sereneseasonsplus$getSurfaceHeight());
        boolean coldEnough = CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(
                level,
                chunkPos.getMiddleBlockPosition(sampleY)
        );

        // Local temperature wins over the broad seasonal policy. A warm chunk
        // must not be sent with stale SSP-owned snow still in it.
        if (!coldEnough && (stateService.hasTrackedSnow(tracked)
                || !tracked.sereneseasonsplus$getIceColumns().isEmpty())) {
            return CommonSnowBlockFeature.meltSnowInChunkImmediately(level, chunk);
        }

        SnowAccumulationPolicy.ChunkDecision decision = CommonSnowBlockFeature.SNOW_ACCUMULATION_POLICY.evaluateChunk(
                level,
                currentSeason,
                tracked,
                chunkPos,
                true,
                tracked.sereneseasonsplus$getSurfaceHeight(),
                coldEnough
        );

        if (decision.action() == SnowAccumulationPolicy.Action.MELT) {
            return CommonSnowBlockFeature.meltSnowInChunkImmediately(level, chunk);
        }
        if (decision.action() != SnowAccumulationPolicy.Action.APPLY) {
            return false;
        }

        boolean changed = CommonSnowBlockFeature.applySnowForCurrentStormCountImmediately(level, chunk);
        boolean snowStatePresent = stateService.hasTrackedSnow(tracked);
        if (CommonSnowBlockFeature.hasApplicableStormRecord(level) && (changed || snowStatePresent)) {
            tracked.sereneseasonsplus$setAppliedStormCount(
                    CommonSnowBlockFeature.HANDLER.getSnowStormsThisWinter(level)
            );
            CommonSnowBlockFeature.markSnowSyncSatisfied(tracked);
            chunk.markUnsaved();
        }
        return changed;
    }

    private void initializeChunkMetadata(ServerLevel level, LevelChunk chunk, ISnowTrackedChunk tracked) {
        if (tracked.sereneseasonsplus$getSurfaceHeight() == -1) {
            int surfaceHeight = level.getHeight(
                    Heightmap.Types.WORLD_SURFACE,
                    chunk.getPos().getMiddleBlockX(),
                    chunk.getPos().getMiddleBlockZ()
            );
            tracked.sereneseasonsplus$setSurfaceHeight(surfaceHeight);
            chunk.markUnsaved();
        }

        // AvailableSnowColumns is legacy metadata and has no consumers. Do not
        // spend another full 16x16 surface pass computing it during chunk load.
    }
}

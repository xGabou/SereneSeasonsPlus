package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SnowChunkLoadReconciler {
    private final SnowStateService stateService;
    private final Queue<ChunkPos> pendingLoads = new ConcurrentLinkedQueue<>();
    private final Set<Long> queuedChunkKeys = ConcurrentHashMap.newKeySet();

    public SnowChunkLoadReconciler(SnowStateService stateService) {
        this.stateService = stateService;
    }

    public void enqueue(LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        long key = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        if (queuedChunkKeys.add(key)) {
            pendingLoads.add(chunkPos);
        }
    }

    public void clear() {
        pendingLoads.clear();
        queuedChunkKeys.clear();
    }

    public boolean hasPendingLoads() {
        return !pendingLoads.isEmpty();
    }

    public int process(ServerLevel level, SnowBlockCompatibility compatibility, int minChunks, int maxChunks, long deadlineNanos) {
        ChunkPos chunkPos;
        int processed = 0;
        while ((chunkPos = pendingLoads.poll()) != null) {
            queuedChunkKeys.remove(ChunkPos.asLong(chunkPos.x, chunkPos.z));
            if (processed >= minChunks && (processed >= maxChunks || System.nanoTime() >= deadlineNanos)) {
                requeue(chunkPos);
                break;
            }

            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }

            LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            if (!(chunk instanceof ISnowTrackedChunk tracked)) {
                continue;
            }

            initializeChunkMetadata(level, chunk, tracked, compatibility);
            scheduleInitialReconciliation(level, chunk, tracked);
            processed++;
        }
        return processed;
    }

    private void requeue(ChunkPos chunkPos) {
        long key = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        if (queuedChunkKeys.add(key)) {
            pendingLoads.add(chunkPos);
        }
    }

    private void initializeChunkMetadata(ServerLevel level,
                                         LevelChunk chunk,
                                         ISnowTrackedChunk tracked,
                                         SnowBlockCompatibility compatibility) {
        if (tracked.sereneseasonsplus$getSurfaceHeight() == -1) {
            int surfaceHeight = level.getHeight(
                    Heightmap.Types.WORLD_SURFACE,
                    chunk.getPos().getMiddleBlockX(),
                    chunk.getPos().getMiddleBlockZ()
            );
            tracked.sereneseasonsplus$setSurfaceHeight(surfaceHeight);
        }

        if (tracked.sereneseasonsplus$getAvailableSnowColumns() == -1) {
            tracked.sereneseasonsplus$setAvailableSnowColumns(
                    SnowColumnInspector.countAvailableColumns(level, chunk, compatibility)
            );
        }
    }

    private void scheduleInitialReconciliation(ServerLevel level, LevelChunk chunk, ISnowTrackedChunk tracked) {
        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
        var seasonState = SeasonHelper.getSeasonState(level);
        if (currentSeason == null || seasonState == null) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        int sampleY = Math.max(level.getMinBuildHeight(), tracked.sereneseasonsplus$getSurfaceHeight());
        boolean coldEnough = CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(
                level,
                chunkPos.getMiddleBlockPosition(sampleY)
        );

        if (stateService.hasTrackedSnow(tracked)) {
            if (coldEnough) {
                CommonSnowBlockFeature.enqueueChunkForSnowApply(chunkPos, currentSeason);
            } else {
                CommonSnowBlockFeature.enqueueChunkForSnowMelt(chunkPos, false);
            }
            return;
        }

        SnowLogic.evaluate(
                level,
                currentSeason,
                seasonState,
                tracked,
                chunkPos,
                true,
                tracked.sereneseasonsplus$getSurfaceHeight()
        );
    }
}

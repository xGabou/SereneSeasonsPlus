package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.Gabou.gaboulibs.storage.SnowRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class SnowChunkApplyService {
    private final SnowHistoryQueryService historyQueryService;
    private final SnowStateService stateService;

    public SnowChunkApplyService(SnowHistoryQueryService historyQueryService, SnowStateService stateService) {
        this.historyQueryService = historyQueryService;
        this.stateService = stateService;
    }

    public boolean applySnowHistoryPass(ServerLevel level, LevelChunk chunk) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        int cap = level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        if (cap > 0 && isChunkAtOrAboveSnowCap(level, chunk, cap)) {
            return false;
        }

        int baseline = historyQueryService.computeGlobalMinSum(level);
        if (baseline <= 0) return false;

        boolean any = false;
        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                SnowColumnInspector.ColumnSnapshot snapshot =
                        SnowColumnInspector.inspectColumn(level, x, z, CommonSnowBlockFeature.SNOW_COMPATIBILITY);
                if (snapshot == null) continue;

                int current = snapshot.totalLayers();
                int need = baseline - current;
                if (cap > 0) {
                    int remainingCap = cap - current;
                    if (remainingCap <= 0) continue;
                    if (need > remainingCap) need = remainingCap;
                }
                if (need <= 0) continue;

                int remaining = need;
                BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos(
                        snapshot.anchorPos().getX(),
                        snapshot.anchorPos().getY(),
                        snapshot.anchorPos().getZ()
                );

                if (snapshot.topManagedPos() != null) {
                    cursor.set(snapshot.topManagedPos().getX(), snapshot.topManagedPos().getY(), snapshot.topManagedPos().getZ());
                    BlockState topState = level.getBlockState(cursor);
                    int currentLayers = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getManagedLayers(level, cursor.immutable(), topState);
                    int freeSpace = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getMaxManagedLayers(level, cursor.immutable(), topState) - currentLayers;
                    if (freeSpace > 0 && remaining > 0) {
                        int add = Math.min(freeSpace, remaining);
                        int targetLayers = currentLayers + add;
                        if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, targetLayers, true, true)) {
                            stateService.setTrackedLayers(tracked, cursor.immutable(), targetLayers);
                            any = true;
                        }
                        remaining -= add;
                    }
                    if (remaining <= 0) continue;
                    placePos.set(cursor.getX(), cursor.getY() + 1, cursor.getZ());
                }

                while (remaining > 0 && placePos.getY() < level.getMaxY()) {
                    int toPlace = Math.min(8, remaining);
                    if (CommonSnowBlockFeature.placeOrQueueLayers(level, placePos, toPlace, true, true)) {
                        stateService.setTrackedLayers(tracked, placePos.immutable(), toPlace);
                        any = true;
                    }
                    remaining -= toPlace;
                    placePos.move(0, 1, 0);
                }
            }
        }

        SnowHistorySavedData savedData = SnowHistorySavedData.get();
        if (savedData != null) {
            if (savedData.currentStormId > 0) {
                SnowRecord activeRecord = savedData.snowHistory.get(savedData.currentStormId);
                if (activeRecord != null) {
                    return applySnowPattern(level, chunk, activeRecord, level.random) || any;
                }
            } else {
                SnowRecord combined = historyQueryService.aggregateFinishedStormSums(level);
                if (combined != null) {
                    return applyCombinedFinishedPattern(level, chunk, combined, level.random) || any;
                }
            }
        }
        return any;
    }

    public boolean applySnowPatternFromActiveRecord(ServerLevel level, LevelChunk chunk) {
        SnowHistorySavedData savedData = SnowHistorySavedData.get();
        if (savedData != null && savedData.currentStormId > 0) {
            SnowRecord record = savedData.snowHistory.get(savedData.currentStormId);
            if (record != null) {
                return applySnowPattern(level, chunk, record, level.random);
            }
        }
        return false;
    }

    public boolean applySnowPattern(ServerLevel level, LevelChunk chunk, SnowRecord record, RandomSource random) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        int cap = level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        if (cap > 0 && isChunkAtOrAboveSnowCap(level, chunk, cap)) {
            return false;
        }

        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();

        float avg = Math.max(0f, record.avgLayers);
        float coverage = Mth.clamp(avg / 8f, 0.10f, 0.85f);
        boolean any = false;

        float progress = 1.0f;
        int currentTick = CommonSnowBlockFeature.getTickCounter();
        if (!CommonSnowBlockFeature.FAST_PILING_MODE) {
            SnowHistorySavedData savedData = SnowHistorySavedData.get();
            int activeId = savedData != null ? savedData.currentStormId : 0;
            if (activeId > 0) {
                if (tracked.sereneseasonsplus$getStormIdApplied() != activeId) {
                    tracked.sereneseasonsplus$setStormIdApplied(activeId);
                    tracked.sereneseasonsplus$setStormProgress(0f);
                    tracked.sereneseasonsplus$setLastProgressTick(currentTick);
                }
                float current = tracked.sereneseasonsplus$getStormProgress();
                int last = tracked.sereneseasonsplus$getLastProgressTick();
                int deltaTicks = Math.max(1, currentTick - last);
                float delta = (float) deltaTicks / (float) Math.max(1, CommonSnowBlockFeature.ACTIVE_STORM_TARGET_TICKS);
                current = Mth.clamp(
                        current + (delta * CommonSnowBlockFeature.STORM_INTENSITY_MULTIPLIER),
                        0f,
                        1f
                );
                tracked.sereneseasonsplus$setStormProgress(current);
                tracked.sereneseasonsplus$setLastProgressTick(currentTick);
                progress = current;
            }
        }

        int activeId = 0;
        SnowHistorySavedData localData = SnowHistorySavedData.get();
        if (localData != null) {
            activeId = localData.currentStormId;
        }

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                if (activeId > 0) {
                    if (stateService.isDestroyedDuringStorm(tracked, activeId, x, z)) {
                        continue;
                    }
                }

                float white = random.nextFloat();
                double wave = Math.sin((x * 0.12D) + (z * 0.12D));
                float noise = Mth.clamp((float) ((white * 0.7) + ((wave + 1.0) * 0.15)), 0f, 1f);
                if (noise > coverage) continue;

                SnowColumnInspector.ColumnSnapshot snapshot =
                        SnowColumnInspector.inspectColumn(level, x, z, CommonSnowBlockFeature.SNOW_COMPATIBILITY);
                if (snapshot == null) continue;

                int minLayers = Math.max(1, Math.round(record.minLayers));
                int maxLayers = Math.max(minLayers, Math.round(record.maxLayers));
                int span = Math.max(1, maxLayers - minLayers + 1);
                int pick = minLayers + random.nextInt(span);
                int towardAvg = Math.round(Mth.lerp(0.35f, pick, avg));
                int totalLayers = Math.max(1, towardAvg);
                int desired = CommonSnowBlockFeature.FAST_PILING_MODE
                        ? totalLayers
                        : Math.max(0, Math.round(totalLayers * progress));
                if (desired <= 0) continue;

                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                        snapshot.anchorPos().getX(),
                        snapshot.anchorPos().getY(),
                        snapshot.anchorPos().getZ()
                );
                int currentTotal = snapshot.totalLayers();
                int need = desired - currentTotal;
                if (cap > 0) {
                    int remaining = cap - currentTotal;
                    if (remaining <= 0) continue;
                    if (need > remaining) need = remaining;
                }
                if (need <= 0) continue;

                if (snapshot.topManagedPos() != null) {
                    cursor.set(snapshot.topManagedPos().getX(), snapshot.topManagedPos().getY(), snapshot.topManagedPos().getZ());
                    BlockState existing = level.getBlockState(cursor);
                    int currentLayers = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getManagedLayers(level, cursor.immutable(), existing);
                    int freeSpace = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getMaxManagedLayers(level, cursor.immutable(), existing) - currentLayers;
                    if (freeSpace > 0 && need > 0) {
                        int add = Math.min(freeSpace, need);
                        int targetLayers = currentLayers + add;
                        if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, targetLayers, true, true)) {
                            stateService.setTrackedLayers(tracked, cursor.immutable(), targetLayers);
                            any = true;
                        }
                        need -= add;
                    }
                    if (need > 0) {
                        cursor.move(0, 1, 0);
                    }
                }

                while (need >= 8 && cursor.getY() < level.getMaxY()) {
                    if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, 8, true, true)) {
                        stateService.setTrackedLayers(tracked, cursor.immutable(), 8);
                        any = true;
                    }
                    need -= 8;
                    cursor.move(0, 1, 0);
                }

                if (need > 0 && cursor.getY() < level.getMaxY()) {
                    if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, need, true, true)) {
                        stateService.setTrackedLayers(tracked, cursor.immutable(), need);
                        any = true;
                    }
                }
            }
        }

        return any;
    }

    public boolean applyCombinedFinishedPattern(ServerLevel level, LevelChunk chunk, SnowRecord combined, RandomSource random) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        int cap = level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        if (cap > 0 && isChunkAtOrAboveSnowCap(level, chunk, cap)) {
            return false;
        }

        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();

        int minLayers = Math.max(0, Math.round(combined.minLayers));
        int avgLayers = Math.max(minLayers, Math.round(combined.avgLayers));
        int maxLayers = Math.max(avgLayers, Math.round(combined.maxLayers));
        boolean any = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                float white = random.nextFloat();
                double wave = Math.sin((x * 0.12D) + (z * 0.12D));
                float noise = Mth.clamp((float) ((white * 0.7) + ((wave + 1.0) * 0.15)), 0f, 1f);

                int pick = minLayers + Math.round(noise * Math.max(0, maxLayers - minLayers));
                int towardAvg = Math.round(Mth.lerp(0.35f, pick, avgLayers));
                int desired = Math.max(minLayers, towardAvg);
                if (desired <= 0) continue;

                SnowColumnInspector.ColumnSnapshot snapshot =
                        SnowColumnInspector.inspectColumn(level, x, z, CommonSnowBlockFeature.SNOW_COMPATIBILITY);
                if (snapshot == null) continue;

                int currentTotal = snapshot.totalLayers();
                int need = desired - currentTotal;
                if (cap > 0) {
                    int remaining = cap - currentTotal;
                    if (remaining <= 0) continue;
                    if (need > remaining) need = remaining;
                }
                if (need <= 0) continue;

                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                        snapshot.anchorPos().getX(),
                        snapshot.anchorPos().getY(),
                        snapshot.anchorPos().getZ()
                );
                if (snapshot.topManagedPos() != null) {
                    cursor.set(snapshot.topManagedPos().getX(), snapshot.topManagedPos().getY(), snapshot.topManagedPos().getZ());
                    BlockState existing = level.getBlockState(cursor);
                    int currentLayers = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getManagedLayers(level, cursor.immutable(), existing);
                    int freeSpace = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getMaxManagedLayers(level, cursor.immutable(), existing) - currentLayers;
                    if (freeSpace > 0 && need > 0) {
                        int add = Math.min(freeSpace, need);
                        int targetLayers = currentLayers + add;
                        if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, targetLayers, true, true)) {
                            stateService.setTrackedLayers(tracked, cursor.immutable(), targetLayers);
                            any = true;
                        }
                        need -= add;
                    }
                    if (need > 0) {
                        cursor.move(0, 1, 0);
                    }
                }

                while (need >= 8 && cursor.getY() < level.getMaxY()) {
                    if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, 8, true, true)) {
                        stateService.setTrackedLayers(tracked, cursor.immutable(), 8);
                        any = true;
                    }
                    need -= 8;
                    cursor.move(0, 1, 0);
                }

                if (need > 0 && cursor.getY() < level.getMaxY()) {
                    if (CommonSnowBlockFeature.placeOrQueueLayers(level, cursor, need, true, true)) {
                        stateService.setTrackedLayers(tracked, cursor.immutable(), need);
                        any = true;
                    }
                }
            }
        }
        return any;
    }

    public boolean isChunkAtOrAboveSnowCap(ServerLevel level, LevelChunk chunk, int capLayers) {
        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                SnowColumnInspector.ColumnSnapshot snapshot =
                        SnowColumnInspector.inspectColumn(level, baseX + dx, baseZ + dz, CommonSnowBlockFeature.SNOW_COMPATIBILITY);
                if (snapshot != null && snapshot.totalLayers() >= capLayers) {
                    return true;
                }
            }
        }
        return false;
    }
}

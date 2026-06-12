package com.Gabou.sereneseasonsplus.access;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;

public interface ISnowTrackedChunk {
    int sereneseasonsplus$getLastWinterId();
    void sereneseasonsplus$setLastWinterId(int id);

    // Per-chunk snow columns: maps a block position to its current snow layer count
    Map<BlockPos, Integer> sereneseasonsplus$getSnowColumns();

    default int sereneseasonsplus$getTotalSnowLayers() {
        int sum = 0;
        for (int value : sereneseasonsplus$getSnowColumns().values()) {
            sum += value;
        }
        return sum;
    }

    default int sereneseasonsplus$getTrackedColumnCount() {
        return sereneseasonsplus$getSnowColumns().size();
    }


    int sereneseasonsplus$getSurfaceHeight();

    void sereneseasonsplus$setSurfaceHeight(int height);

    // Estimated number of columns in this chunk where snow can accumulate (0..256). -1 = unknown
    int sereneseasonsplus$getAvailableSnowColumns();
    void sereneseasonsplus$setAvailableSnowColumns(int count);

    // Active-storm progress tracking per chunk (0..1)
    float sereneseasonsplus$getStormProgress();
    void sereneseasonsplus$setStormProgress(float progress);

    // Storm id the progress is for; helps reset when a new storm starts
    int sereneseasonsplus$getStormIdApplied();
    void sereneseasonsplus$setStormIdApplied(int id);

    // Last tick count when progress was updated; used to scale by time
    int sereneseasonsplus$getLastProgressTick();
    void sereneseasonsplus$setLastProgressTick(int tick);

    int sereneseasonsplus$getSnowSyncGeneration();
    void sereneseasonsplus$setSnowSyncGeneration(int generation);

    int sereneseasonsplus$getAppliedStormCount();
    void sereneseasonsplus$setAppliedStormCount(int count);

    // Positions of ice we froze (rivers/oceans) to thaw efficiently later
    Set<BlockPos> sereneseasonsplus$getIceColumns();

    // Per-storm: columns (x,z) where snow was destroyed by players during the active storm
    // Encoded as a set of packed longs: (x << 32) ^ (z & 0xffffffffL)
    int sereneseasonsplus$getDestroyedStormId();
    void sereneseasonsplus$setDestroyedStormId(int id);
    java.util.Set<Long> sereneseasonsplus$getDestroyedColumns();
}

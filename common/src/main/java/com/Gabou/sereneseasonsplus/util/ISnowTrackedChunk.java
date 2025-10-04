package com.Gabou.sereneseasonsplus.util;

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

    // Active-storm progress tracking per chunk (0..1)
    float sereneseasonsplus$getStormProgress();
    void sereneseasonsplus$setStormProgress(float progress);

    // Storm id the progress is for; helps reset when a new storm starts
    int sereneseasonsplus$getStormIdApplied();
    void sereneseasonsplus$setStormIdApplied(int id);

    // Last tick count when progress was updated; used to scale by time
    int sereneseasonsplus$getLastProgressTick();
    void sereneseasonsplus$setLastProgressTick(int tick);

    // Positions of ice we froze (rivers/oceans) to thaw efficiently later
    Set<BlockPos> sereneseasonsplus$getIceColumns();
}

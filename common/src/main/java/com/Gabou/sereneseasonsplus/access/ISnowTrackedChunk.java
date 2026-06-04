package com.Gabou.sereneseasonsplus.access;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Set;

public interface ISnowTrackedChunk {
    int sereneseasonsplus$getLastWinterId();
    void sereneseasonsplus$setLastWinterId(int id);

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

    int sereneseasonsplus$getAvailableSnowColumns();
    void sereneseasonsplus$setAvailableSnowColumns(int count);

    float sereneseasonsplus$getStormProgress();
    void sereneseasonsplus$setStormProgress(float progress);

    int sereneseasonsplus$getStormIdApplied();
    void sereneseasonsplus$setStormIdApplied(int id);

    int sereneseasonsplus$getLastProgressTick();
    void sereneseasonsplus$setLastProgressTick(int tick);

    Set<BlockPos> sereneseasonsplus$getIceColumns();

    int sereneseasonsplus$getDestroyedStormId();
    void sereneseasonsplus$setDestroyedStormId(int id);
    Set<Long> sereneseasonsplus$getDestroyedColumns();
}

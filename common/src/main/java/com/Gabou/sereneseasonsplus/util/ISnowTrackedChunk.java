package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;

import java.util.Map;

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
}


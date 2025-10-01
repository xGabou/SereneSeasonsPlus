package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import java.util.Map;

public interface ISnowTrackedChunk {
    // Stores the last winterId this chunk was processed for
    int sereneseasonsplus$getLastWinterId();
    void sereneseasonsplus$setLastWinterId(int id);

    // Per-chunk snow columns: maps a block position to its current snow layer count
    Map<BlockPos, Integer> sereneseasonsplus$getSnowColumns();

    // Convenience defaults for aggregate info
    default int sereneseasonsplus$getSnowColumnsTotalLayers() {
        int sum = 0;
        for (int v : sereneseasonsplus$getSnowColumns().values()) sum += v;
        return sum;
    }

    default int sereneseasonsplus$getSnowColumnsCount() {
        return sereneseasonsplus$getSnowColumns().size();
    }
}

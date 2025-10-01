package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import java.util.Map;

public interface ISnowTrackedChunk {
    // Tracks whether the chunk should receive deep-winter initialization snow
    boolean sereneseasonsplus$shouldApplyInitialSnow();
    void sereneseasonsplus$setShouldApplyInitialSnow(boolean value);

    // Remembers if deep-winter initialization snow has already been applied
    boolean sereneseasonsplus$hasAppliedInitialSnow();
    void sereneseasonsplus$setHasAppliedInitialSnow(boolean value);

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

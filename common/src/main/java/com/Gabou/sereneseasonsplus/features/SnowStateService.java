package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public final class SnowStateService {
    public Map<BlockPos, Integer> getSnowColumns(ISnowTrackedChunk tracked) {
        return tracked.sereneseasonsplus$getSnowColumns();
    }

    public boolean hasTrackedSnow(ISnowTrackedChunk tracked) {
        return !tracked.sereneseasonsplus$getSnowColumns().isEmpty();
    }

    public void setTrackedLayers(ISnowTrackedChunk tracked, BlockPos pos, int layers) {
        if (layers <= 0) {
            tracked.sereneseasonsplus$getSnowColumns().remove(pos);
            return;
        }
        tracked.sereneseasonsplus$getSnowColumns().put(pos.immutable(), Mth.clamp(layers, 1, 8));
    }

    public void removeTrackedColumn(ISnowTrackedChunk tracked, BlockPos pos) {
        tracked.sereneseasonsplus$getSnowColumns().remove(pos);
    }

    public Map<Long, BlockPos> getTopTrackedSnowByColumn(ISnowTrackedChunk tracked) {
        Map<Long, BlockPos> topByColumn = new HashMap<>();
        for (BlockPos pos : tracked.sereneseasonsplus$getSnowColumns().keySet()) {
            long key = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
            BlockPos current = topByColumn.get(key);
            if (current == null || pos.getY() > current.getY()) {
                topByColumn.put(key, pos.immutable());
            }
        }
        return topByColumn;
    }

    public int getAvailableSnowColumnsOrDefault(ISnowTrackedChunk tracked, int fallback) {
        int available = tracked.sereneseasonsplus$getAvailableSnowColumns();
        return available > 0 ? available : fallback;
    }

    public void ensureDestroyedColumnsBoundToStorm(ISnowTrackedChunk tracked, int stormId) {
        if (stormId <= 0) {
            return;
        }
        if (tracked.sereneseasonsplus$getDestroyedStormId() != stormId) {
            tracked.sereneseasonsplus$getDestroyedColumns().clear();
            tracked.sereneseasonsplus$setDestroyedStormId(stormId);
        }
    }

    public boolean isDestroyedDuringStorm(ISnowTrackedChunk tracked, int stormId, int x, int z) {
        if (stormId <= 0) {
            return false;
        }
        ensureDestroyedColumnsBoundToStorm(tracked, stormId);
        long xz = (((long) x) << 32) ^ (z & 0xffffffffL);
        return tracked.sereneseasonsplus$getDestroyedColumns().contains(xz);
    }
}

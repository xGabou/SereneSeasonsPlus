package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Vanilla-compatible rain handler with simple per-level caching.
 * Checks global rain state once every 100 server ticks.
 */
public class DefaultRainHandler implements IRainHandler {
    private static final int CACHE_INTERVAL_TICKS = 100;

    private static final class CacheEntry {
        int lastTick;
        boolean lastValue;
    }

    private final Map<ServerLevel, CacheEntry> cache = new HashMap<>();

    @Override
    public boolean isRainingAt(ServerLevel level, BlockPos pos) {
        CacheEntry e = cache.computeIfAbsent(level, k -> new CacheEntry());
        int tick = com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature.getTickCounter();
        if (tick - e.lastTick >= CACHE_INTERVAL_TICKS) {
            e.lastValue = queryPrecipitation(level, pos);
            e.lastTick = tick;
        }
        return e.lastValue;
    }

    /**
     * Computes the current precipitation state for the supplied level/position.
     * Subclasses can override to integrate with other weather systems.
     */
    protected boolean queryPrecipitation(ServerLevel level, BlockPos pos) {
        return level.isRaining();
    }
}


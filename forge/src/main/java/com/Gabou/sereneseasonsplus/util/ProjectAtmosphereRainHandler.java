package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Rain handler for Project Atmosphere: use per-position precipitation checks.
 */
public class ProjectAtmosphereRainHandler implements IRainHandler {

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
            e.lastValue = level.isRainingAt(pos); // vanilla: global precipitation
            e.lastTick = tick;
        }
        return e.lastValue;

    }
}


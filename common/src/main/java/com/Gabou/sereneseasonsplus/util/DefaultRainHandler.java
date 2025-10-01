package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.storage.SnowRecord;
import sereneseasons.season.SeasonHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Vanilla-compatible rain handler with simple caching
 * + storm history trigger when rain starts/stops.
 */
public class DefaultRainHandler implements IRainHandler {
    protected static final int CACHE_INTERVAL_TICKS = 100;

    protected int lastTick = -1;
    protected boolean lastValue = false; // last known raining

    @Override
    public boolean isRainingAt(ServerLevel level, BlockPos pos) {
        int tick = CommonSnowBlockFeature.getTickCounter();

        if (tick - lastTick >= CACHE_INTERVAL_TICKS) {
            boolean currently = isRaining(level, pos);
            if (currently != lastValue) {
                // Rain state changed → act like onRainChanged
                handleRainChange(level, pos, currently);
            }
            lastValue = currently;
            lastTick = tick;
        }
        return lastValue;
    }

    protected boolean isRaining(ServerLevel level, BlockPos pos) {
        return level.isRaining();
    }

    @Override
    public void handleRainChange(ServerLevel level, BlockPos pos, boolean isRaining) {
        ChunkPos chunkPos = new ChunkPos(pos);

        if (isRaining && SeasonHooks.coldEnoughToSnowSeasonal(level, pos)) {
                        ChunkQueue.enqueueApply(chunkPos);

        } else if (!isRaining) {
            // Storm ended → finalize record with randomized values
            
        }
    }

}



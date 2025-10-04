package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Vanilla-compatible rain handler.
 * - Global: tracks last raining state per-level to detect changes.
 * - Local: supports per-position precipitation checks (simple in vanilla).
 */
public class DefaultRainHandler implements IRainHandler {
    protected static class State {
        boolean lastValue;
    }

    protected final Map<ServerLevel, State> states = new HashMap<>();

    /**
     * Called each tick to detect transitions in raining state.
     * If the state changes, you can trigger onRainChanged from outside.
     */
    @Override
    public void checkAndUpdate(ServerLevel level) {
        State s = states.computeIfAbsent(level, k -> new State());
        boolean now = level.isRaining();
        if (now != s.lastValue) {
            s.lastValue = now;
            CommonSnowBlockFeature.HANDLER.onRainChanged(level, now);
        }
    }


    @Override
    public void onSimpleCloudsSpawned(ServerLevel level,int hashCode) {


    }
    @Override
    public void onSimpleCloudsDespawned(ServerLevel level, int hashCode) {

    }

    /**
     * Vanilla just uses global rain, ignoring pos.
     * For PA you can override to check region-based precipitation.
     */
    @Override
    public boolean isRainingAt(ServerLevel level, BlockPos pos) {
        return level.isRaining();
    }
}

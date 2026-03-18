package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import net.Gabou.projectatmosphere.api.AtmoApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rain handler for Project Atmosphere: driven by cloud spawn/despawn events.
 */
public class ProjectAtmosphereRainHandler extends DefaultRainHandler {

    private final Map<ServerLevel, PaState> states = new HashMap<>();

    @Override
    public boolean isRainingAt(ServerLevel level, BlockPos pos) {
        try {
            return AtmoApi.getInstance().isRainningAt(level, pos);
        } catch (Throwable t) {
            PaState s = states.get(level);
            if (s == null) {
                return AtmoApi.getInstance().isRainingOrThundering(level, pos);
            }
            return !s.activeRainClouds.isEmpty();
        }
    }

    /**
     * Not used: rain state is driven by cloud events.
     */
    @Override
    public void checkAndUpdate(ServerLevel level) {
        // NO-OP
    }

    /**
     * Called when a cloud spawns.
     * If it's rainy, add it to the active set and update rain state.
     */
    public void onSimpleCloudsSpawned(ServerLevel level, int cloudId) {
        PaState s = states.computeIfAbsent(level, k -> new PaState());

        boolean wasRaining = !s.activeRainClouds.isEmpty();
        s.activeRainClouds.add(cloudId);

        if (!wasRaining && !s.activeRainClouds.isEmpty()) {
            // transitioned from dry → raining
            CommonSnowBlockFeature.HANDLER.onRainCloudSpawned(level, cloudId);
            s.lastValue = true;
        }
    }

    /**
     * Called when a cloud despawns.
     * If it was rainy, remove it and update rain state.
     */
    public void onSimpleCloudsDespawned(ServerLevel level, int cloudId) {
        PaState s = states.get(level);
        if (s == null) return;

        boolean wasRaining = !s.activeRainClouds.isEmpty();
        s.activeRainClouds.remove(cloudId);

        if (wasRaining && s.activeRainClouds.isEmpty()) {
            // transitioned from raining → dry
            CommonSnowBlockFeature.HANDLER.onRainCloudDespawned(level, cloudId);
            s.lastValue = false;

        }
    }

    protected final class PaState {
        boolean lastValue; // last global raining state
        final Set<Integer> activeRainClouds = new HashSet<>(); // currently rainy clouds
    }
}

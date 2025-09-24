package com.Gabou.sereneseasonsplus.features.logic;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks whether the first storm of a winter has completed per dimension.
 */
public final class WinterFlags {
    private WinterFlags() {}

    private static final Map<ResourceKey<Level>, Map<Integer, Boolean>> FIRST_STORM_DONE = new ConcurrentHashMap<>();

    public static boolean hasFirstStormFinished(ServerLevel level, int winterId) {
        Map<Integer, Boolean> byWinter = FIRST_STORM_DONE.get(level.dimension());
        if (byWinter == null) return false;
        return Boolean.TRUE.equals(byWinter.get(winterId));
    }

    public static void markFirstStormFinished(ServerLevel level, int winterId) {
        FIRST_STORM_DONE
                .computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                .put(winterId, true);
    }

    /** Optional utility if you ever want to wipe old entries */
    public static Map<Integer, Boolean> viewRaw(ResourceKey<Level> dim) {
        Map<Integer, Boolean> m = FIRST_STORM_DONE.get(dim);
        return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
    }
}

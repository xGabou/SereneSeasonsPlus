package com.Gabou.sereneseasonsplus.util;

import net.minecraft.server.level.ServerLevel;

/**
 * Holds a reference to the overworld {@link ServerLevel} so common code
 * can access per-world saved data without requiring a level parameter.
 *
 * The reference is set on server start and cleared on server stop by
 * {@link EnvironmentHelper}.
 */
public final class WorldContext {
    private static volatile ServerLevel OVERWORLD;

    private WorldContext() {}

    /** Called when the server overworld becomes available. */
    public static void setOverworld(ServerLevel level) {
        OVERWORLD = level;
    }

    /** Returns the current overworld level, or null if not available. */
    public static ServerLevel getOverworld() {
        return OVERWORLD;
    }

    /** Clears the stored overworld reference (server stopping). */
    public static void clear() {
        OVERWORLD = null;
    }
}


package com.Gabou.sereneseasonsplus.features.logic;

import net.minecraft.server.level.ServerLevel;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;

/**
 * Tracks whether the first storm of the current winter has finished.
 * Simplified: only considers the overworld and one winterId at a time.
 */
public final class WinterFlags {
    private WinterFlags() {}

    private static int lastWinterId = -1;
    private static boolean firstStormFinished = false;

    public static boolean hasFirstStormFinished(ServerLevel level) {
        int currentWinter = EnvironmentHelper.getCurrentWinterId();
        if (lastWinterId != currentWinter) {
            // reset when winter changes
            lastWinterId = currentWinter;
            firstStormFinished = false;
        }
        return firstStormFinished;
    }

    public static void markFirstStormFinished(ServerLevel level) {
        int currentWinter = EnvironmentHelper.getCurrentWinterId();
        if (lastWinterId != currentWinter) {
            lastWinterId = currentWinter;
        }
        firstStormFinished = true;
    }

    /** Debug utility */
    public static boolean viewRaw() {
        return firstStormFinished;
    }
}

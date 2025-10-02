package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Abstraction for platform-specific rain/snow detection.
 * Implementations may cache results.
 */
public interface IRainHandler {
    /**
     * Returns whether it is precipitating (raining/snowing) relevant to the given position.
     */
    boolean isRainingAt(ServerLevel level, BlockPos pos);

    void onSimpleCloudsSpawned(ServerLevel level, int hashCode);
    void onSimpleCloudsDespawned(ServerLevel level, int hashCode);

    void checkAndUpdate(ServerLevel level);
}


package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Loader specific hooks used to align snow placement with the active weather
 * system.  The default implementation keeps track of the chunks that received
 * natural snowfall so the common snow logic can avoid blanketing the world the
 * moment winter begins.
 */
public interface ISnowEnvironmentHandler {
    int getBlocksToReplace(ServerLevel level, BlockPos playerPos);

    /**
     * Clears any per-level bookkeeping for the newly detected winter.
     */
    void resetWinterState(ServerLevel level, int winterId);

    /**
     * Called whenever the rain/snow state for a chunk transitions.
     */
    void onRainChanged(ServerLevel level, boolean isRaining);

    /**
     * @return number of storms detected for the active winter on this level.
     */
    int getSnowStormsThisWinter(ServerLevel level);

    /**
     * Clears any cached state for the supplied level (typically on shutdown).
     */
    void clear(ServerLevel level);


    boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos);

    void onRainCloudSpawned(ServerLevel level, int hashCode);

    /** Called when a rainy cloud despawns */
    void onRainCloudDespawned(ServerLevel level, int hashCode);
}

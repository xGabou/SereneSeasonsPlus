package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Loader specific hooks used to align snow placement with the active weather
 * system.  The default implementation keeps track of the chunks that received
 * natural snowfall so the common snow logic can avoid blanketing the world the
 * moment winter begins.
 */
public interface SnowEnvironmentHandler {
    int getBlocksToReplace(ServerLevel level, BlockPos playerPos);

    /**
     * Clears any per-level bookkeeping for the newly detected winter.
     */
    void resetWinterState(ServerLevel level, int winterId);

    /**
     * Called whenever the rain/snow state for a chunk transitions.
     */
    void onRainChanged(ServerLevel level, ChunkPos chunkPos, boolean isRaining, ISnowTrackedChunk trackedChunk);

    /**
     * Returns {@code true} if this chunk should currently receive natural snow
     * (e.g. because a storm has recently passed over it).
     */
    boolean shouldApplySnow(ServerLevel level, ChunkPos chunkPos);

    /**
     * Records the result of an attempted snow application. Successful
     * placements clear any pending flags so subsequent storms can retrigger the
     * chunk.
     */
    void onSnowApplied(ServerLevel level, ChunkPos chunkPos, boolean success);

    /**
     * @return number of storms detected for the active winter on this level.
     */
    int getSnowStormsThisWinter(ServerLevel level);

    /**
     * @return {@code true} if we have observed natural snowfall in this chunk
     * during the current winter.
     */
    boolean hasChunkSeenSnow(ServerLevel level, ChunkPos chunkPos);

    /**
     * Clears any cached state for the supplied level (typically on shutdown).
     */
    void clear(ServerLevel level);


    boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos);
}

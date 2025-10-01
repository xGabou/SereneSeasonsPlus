package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Loader-specific hooks used to align snow placement with the active weather
 * system. Provides the minimum surface needed by the common snow logic.
 */
public interface SnowEnvironmentHandler {
    /**
     * Compute how many blocks should be replaced with snow layers around a player position.
     */
    int getBlocksToReplace(ServerLevel level, BlockPos playerPos);
    /**
     * @return {@code true} if this position is cold enough for snow.
     */
    boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos);

    int getSnowStormsThisWinter(ServerLevel level);
}

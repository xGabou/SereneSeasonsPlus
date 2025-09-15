package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.Season;

public interface SnowEnvironmentHandler {
    int getBlocksToReplace(ServerLevel level, BlockPos playerPos);
    void processChunks(ServerLevel level, BlockPos worldPos, Season.SubSeason currentSubSeason, ChunkPos chunkPos);
}

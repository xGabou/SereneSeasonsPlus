package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface SnowEnvironmentHandler {
    int getBlocksToReplace(ServerLevel level, BlockPos playerPos);
}

package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public interface ISnowHandler {
    int getAttemptCount(ServerLevel level, ChunkPos center);
}

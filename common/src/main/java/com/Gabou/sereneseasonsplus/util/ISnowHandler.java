package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface ISnowHandler {
    int getAttemptCount(ServerLevel level, BlockPos center);
}

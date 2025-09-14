package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class VanillaSnowHandler implements ISnowHandler {
    @Override
    public int getAttemptCount(ServerLevel level, BlockPos center) {
        if (!level.isRaining()) return 0;

        final boolean coldEnough = level.getBiome(center).value().coldEnoughToSnow(center);
        if (!coldEnough) return 0;

        final var rnd = level.random;
        return rnd.nextInt(2) + (level.isThundering() ? rnd.nextInt(2) : 0);
    }
}

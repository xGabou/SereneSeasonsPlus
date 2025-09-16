package com.Gabou.sereneseasonsplus.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class VanillaSnowHandler implements ISnowHandler {
    @Override
    public int getAttemptCount(ServerLevel level, ChunkPos center) {
        if (!level.isRaining()) return 0;

        final boolean coldEnough = level.getBiome(center.getMiddleBlockPosition(65)).value().coldEnoughToSnow(center.getMiddleBlockPosition(65));
        if (!coldEnough) return 0;

        final var rnd = level.random;
        return rnd.nextInt(2) + (level.isThundering() ? rnd.nextInt(2) : 0);
    }
}

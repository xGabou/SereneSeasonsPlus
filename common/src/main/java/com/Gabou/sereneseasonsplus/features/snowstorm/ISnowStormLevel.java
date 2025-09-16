package com.Gabou.sereneseasonsplus.features.snowstorm;

import net.minecraft.world.level.ChunkPos;

public interface ISnowStormLevel {
    boolean sereneseasonsplus$isSnowStormAt(ChunkPos pos);
    int sereneseasonsplus$getSnowStormIntensity(ChunkPos pos);
}


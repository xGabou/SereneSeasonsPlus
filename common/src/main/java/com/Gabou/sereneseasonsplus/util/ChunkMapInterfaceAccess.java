package com.Gabou.sereneseasonsplus.util;

import net.minecraft.server.level.ChunkHolder;

public interface ChunkMapInterfaceAccess {
    Iterable<ChunkHolder> snow$getChunksSafe();
}

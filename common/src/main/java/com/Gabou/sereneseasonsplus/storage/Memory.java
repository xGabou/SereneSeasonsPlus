package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.world.level.ChunkPos;

import java.time.Instant;
import java.util.HashMap;

public class Memory {
    private static final HashMap<ChunkPos, Long> data = new HashMap<>();



    public static void remember(ChunkPos chunkPos) {
        data.put(chunkPos, Instant.now().getEpochSecond());
    }

    public static void forget(ChunkPos chunkPos) {
        data.remove(chunkPos);
    }

    public static boolean hasForgotten(ChunkPos chunkPos) {
        long rememberedTime = data.getOrDefault(chunkPos, 0L);
        long currentTime = Instant.now().getEpochSecond();
        long difference = currentTime - rememberedTime;

        boolean hasForgotten = difference >= MemoryHandler.getForgetTime();
        if (hasForgotten) data.remove(chunkPos); // Clear out so memory doesn't grow forever
        return hasForgotten;
    }

    public static void erase() {
        data.clear();
    }

    public static int getSize() {
        return data.size();
    }

    public static boolean isFull() {
        return getSize() >= MemoryHandler.getMaxSize();
    }
}
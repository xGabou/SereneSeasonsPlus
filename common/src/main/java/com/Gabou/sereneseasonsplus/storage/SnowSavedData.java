package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists per-level snow environment state across world reloads.
 * Standalone storage version — no SavedData dependency.
 */
public class SnowSavedData {
    private static volatile SnowSavedData INSTANCE;
    private static final String FILE_PATH = "world/sereneseasonsplus/snow_data.dat";

    public int winterId = -1;
    public int stormCount = 0;
    public boolean stormActive = false;
    public final Set<Long> pendingChunks = new HashSet<>();
    public final Set<Long> observedChunks = new HashSet<>();
    public int lastBlanketStormCount = 0;

    public SnowSavedData() {}

    // ============================================================
    // Singleton accessor
    // ============================================================
    public static SnowSavedData get() {
        if (INSTANCE != null) return INSTANCE;
        synchronized (SnowSavedData.class) {
            if (INSTANCE == null) {
                INSTANCE = new SnowSavedData();
                INSTANCE.load();
            }
            return INSTANCE;
        }
    }

    // ============================================================
    // Save to file (compressed NBT)
    // ============================================================
    public void save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("WinterId", winterId);
        tag.putInt("StormCount", stormCount);
        tag.putBoolean("StormActive", stormActive);
        tag.putInt("LastBlanketStormCount", lastBlanketStormCount);

        long[] pending = new long[pendingChunks.size()];
        int i = 0;
        for (Long v : pendingChunks) pending[i++] = v;
        tag.putLongArray("PendingChunks", pending);

        long[] observed = new long[observedChunks.size()];
        i = 0;
        for (Long v : observedChunks) observed[i++] = v;
        tag.putLongArray("ObservedChunks", observed);

        Path path = Path.of(FILE_PATH);
        try {
            Files.createDirectories(path.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
                NbtIo.writeCompressed(tag, out);
            }
        } catch (IOException e) {
            System.err.println("[SS+] Failed to save snow data: " + e.getMessage());
        }
    }

    // ============================================================
    // Load from file (compressed NBT)
    // ============================================================
    public void load() {
        Path path = Path.of(FILE_PATH);
        if (!Files.exists(path)) return;

        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());

            if (tag.getInt("WinterId").isPresent()) winterId = tag.getInt("WinterId").get();
            if (tag.getInt("StormCount").isPresent()) stormCount = tag.getInt("StormCount").get();
            if (tag.getBoolean("StormActive").isPresent()) stormActive = tag.getBoolean("StormActive").get();
            if (tag.getInt("LastBlanketStormCount").isPresent()) lastBlanketStormCount = tag.getInt("LastBlanketStormCount").get();

            if (tag.getLongArray("PendingChunks").isPresent()) {
                long[] pending = tag.getLongArray("PendingChunks").get();
                for (long v : pending) pendingChunks.add(v);
            }

            if (tag.getLongArray("ObservedChunks").isPresent()) {
                long[] observed = tag.getLongArray("ObservedChunks").get();
                for (long v : observed) observedChunks.add(v);
            }

        } catch (IOException e) {
            System.err.println("[SS+] Failed to load snow data: " + e.getMessage());
        }
    }

    // ============================================================
    // Clear singleton
    // ============================================================
    public static void clearCachedInstance() {
        INSTANCE = null;
    }
}

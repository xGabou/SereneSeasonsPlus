package com.Gabou.sereneseasonsplus.storage;

import com.Gabou.sereneseasonsplus.util.WorldContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Persists per-level snow environment state across world reloads.
 */
public class SnowSavedData extends SavedData {
    private static volatile SnowSavedData INSTANCE;
    public int winterId = -1;
    public int stormCount = 0;
    public boolean stormActive = false;
    public final Set<Long> pendingChunks = new HashSet<>();
    public final Set<Long> observedChunks = new HashSet<>();
    public int lastBlanketStormCount = 0;

    public SnowSavedData() {}

    public static SnowSavedData load(CompoundTag tag) {
        SnowSavedData data = new SnowSavedData();
        if (tag == null) return data;

        data.winterId = tag.getInt("WinterId");
        data.stormCount = tag.getInt("StormCount");
        data.stormActive = tag.getBoolean("StormActive");
        data.lastBlanketStormCount = tag.getInt("LastBlanketStormCount");

        long[] pending = tag.getLongArray("PendingChunks");
        for (long v : pending) data.pendingChunks.add(v);

        long[] observed = tag.getLongArray("ObservedChunks");
        for (long v : observed) data.observedChunks.add(v);

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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
        return tag;
    }

    /**
     * Returns the singleton SnowSavedData for the overworld. No Level parameter needed.
     * If the overworld is available, this binds to and persists via the world's DataStorage.
     * If not yet available, returns a temporary in-memory instance until the overworld is set.
     */
    public static SnowSavedData get() {
        SnowSavedData inst = INSTANCE;
        if (inst != null) return inst;

        synchronized (SnowSavedData.class) {
            if (INSTANCE != null) return INSTANCE;
            ServerLevel overworld = WorldContext.getOverworld();
            if (overworld != null) {
                INSTANCE = overworld.getDataStorage().computeIfAbsent(
                        new SavedData.Factory<>(
                                SnowSavedData::new,
                                SnowSavedData::load,
                                DataFixTypes.LEVEL // You can also use DataFixTypes.SAVED_DATA
                        ),
                        "ssp_snow_data"
                );
            } else {
                INSTANCE = new SnowSavedData();
            }
            return INSTANCE;
        }
    }

    /** Clears the cached singleton (called on server stop). */
    public static void clearCachedInstance() {
        INSTANCE = null;
    }
}

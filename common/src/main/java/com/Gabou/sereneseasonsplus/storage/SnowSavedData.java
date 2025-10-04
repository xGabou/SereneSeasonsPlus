package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.core.HolderLookup;
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
    public int winterId = -1;
    public int stormCount = 0;
    public boolean stormActive = false;
    public final Set<Long> pendingChunks = new HashSet<>();
    public final Set<Long> observedChunks = new HashSet<>();
    public int lastBlanketStormCount = 0;

    public SnowSavedData() {}

    public static SnowSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
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
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
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

    public static SnowSavedData get(ServerLevel level) {
        SavedData.Factory<SnowSavedData> factory = new SavedData.Factory<>(
                SnowSavedData::new,          // Supplier<T>
                SnowSavedData::load,         // BiFunction<CompoundTag, HolderLookup.Provider, T>
                DataFixTypes.LEVEL     // pick the generic saved data type
        );
        return level.getDataStorage().computeIfAbsent(factory, "ssp_snow_data");
    }
}

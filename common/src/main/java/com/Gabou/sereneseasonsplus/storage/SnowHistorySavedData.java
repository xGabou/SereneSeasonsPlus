package com.Gabou.sereneseasonsplus.storage;

import com.Gabou.sereneseasonsplus.util.WorldContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class SnowHistorySavedData extends SavedData {
    private static volatile SnowHistorySavedData INSTANCE;
    public int currentStormId = 0;
    public final Map<Integer, SnowRecord> snowHistory = new HashMap<>();

    public static SnowHistorySavedData load(CompoundTag tag) {
        SnowHistorySavedData data = new SnowHistorySavedData();
        if (tag == null) return data;
        data.currentStormId = tag.getInt("CurrentStormId");
        ListTag list = tag.getList("SnowHistory", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int id = entry.getInt("Id");
            SnowRecord rec = SnowRecord.fromTag(entry.getCompound("Record"));
            data.snowHistory.put(id, rec);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("CurrentStormId", currentStormId);
        ListTag list = new ListTag();
        for (Map.Entry<Integer, SnowRecord> e : snowHistory.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", e.getKey());
            entry.put("Record", e.getValue().toTag());
            list.add(entry);
        }
        tag.put("SnowHistory", list);
        return tag;
    }

    public static SnowHistorySavedData get() {
        SnowHistorySavedData inst = INSTANCE;
        if (inst != null) return inst;

        synchronized (SnowHistorySavedData.class) {
            if (INSTANCE != null) return INSTANCE;
            ServerLevel overworld = WorldContext.getOverworld();
            if (overworld != null) {
                INSTANCE = overworld.getDataStorage().computeIfAbsent(
                        new SavedData.Factory<>(
                                SnowHistorySavedData::new,
                                SnowHistorySavedData::load,
                                DataFixTypes.LEVEL // for overworld/global data
                        ),
                        "ssp_snow_history"
                );
            } else {
                INSTANCE = new SnowHistorySavedData();
            }
            return INSTANCE;
        }
    }


    /** Clears cached singleton (called on server stop). */
    public static void clearCachedInstance() {
        INSTANCE = null;
    }

}

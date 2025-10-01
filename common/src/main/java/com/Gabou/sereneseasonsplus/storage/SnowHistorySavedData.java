package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class SnowHistorySavedData extends SavedData {
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

    public static SnowHistorySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SnowHistorySavedData::load, SnowHistorySavedData::new, "ssp_snow_history");
    }
}

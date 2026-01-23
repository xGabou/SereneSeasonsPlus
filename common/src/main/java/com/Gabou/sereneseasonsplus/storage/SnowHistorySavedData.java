package com.Gabou.sereneseasonsplus.storage;

import net.Gabou.gaboulibs.storage.SnowRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SnowHistorySavedData {
    private static final String FILE_NAME = "world/sereneseasonsplus/ssp_snow_history.dat";
    private static volatile SnowHistorySavedData INSTANCE;

    public int currentStormId = 0;
    public final Map<Integer, SnowRecord> snowHistory = new HashMap<>();

    // ============================================================
    // Singleton accessor
    // ============================================================
    public static SnowHistorySavedData get() {
        if (INSTANCE != null) return INSTANCE;
        synchronized (SnowHistorySavedData.class) {
            if (INSTANCE == null) {
                INSTANCE = new SnowHistorySavedData();
                INSTANCE.load();
            }
            return INSTANCE;
        }
    }

    // ============================================================
    // Save to NBT file
    // ============================================================
    public void save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CurrentStormId", currentStormId);

        ListTag list = new ListTag();
        for (Map.Entry<Integer, SnowRecord> e : snowHistory.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", e.getKey());
            entry.put("Record", e.getValue().toTag());
            list.add(entry);
        }

        tag.put("SnowHistory", list);

        Path filePath = Path.of(FILE_NAME);
        try {
            Files.createDirectories(filePath.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(filePath))) {
                NbtIo.writeCompressed(tag, out);
            }
        } catch (IOException e) {
            System.err.println("[SS+] Failed to save snow history: " + e.getMessage());
        }
    }

    // ============================================================
    // Load from NBT file
    // ============================================================
    public void load() {
        Path filePath = Path.of(FILE_NAME);
        if (!Files.exists(filePath)) return;

        try (DataInputStream in = new DataInputStream(Files.newInputStream(filePath))) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());

            this.currentStormId = tag.getInt("CurrentStormId").get();

            ListTag list = tag.getList("SnowHistory").get();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i).get();
                int id = entry.getInt("Id").get();
                CompoundTag recTag = entry.getCompound("Record").get();
                this.snowHistory.put(id, SnowRecord.fromTag(recTag));
            }
        } catch (IOException e) {
            System.err.println("[SS+] Failed to load snow history: " + e.getMessage());
        }
    }

    // ============================================================
    // Force clear
    // ============================================================
    public static void clearCachedInstance() {
        INSTANCE = null;
    }
}

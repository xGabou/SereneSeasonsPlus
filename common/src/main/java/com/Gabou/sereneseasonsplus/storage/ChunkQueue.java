package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.Season;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Central queue for one-shot chunk wide snow operations.
 * Tasks are enqueued when a chunk needs an initial snow pass or when
 * a season skip forces a resynchronisation. Entries are removed after
 * a single processing step; there is no incremental work management.
 */
public final class ChunkQueue {
    private static final Queue<Entry> TASKS = new ArrayDeque<>();
    private static final Set<EntryKey> SCHEDULED = new HashSet<>();

    private ChunkQueue() {
    }

    public static void enqueueApply(ChunkPos chunkPos, Season.SubSeason subSeason) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.APPLY_SNOW, false);
        if (SCHEDULED.add(key)) {
            TASKS.add(new Entry(chunkPos, TaskType.APPLY_SNOW, subSeason, false));
        }
    }

    public static void enqueueMelt(ChunkPos chunkPos, boolean fullClear) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.MELT_SNOW, fullClear);
        if (SCHEDULED.add(key)) {
            TASKS.add(new Entry(chunkPos, TaskType.MELT_SNOW, null, fullClear));
        }
    }

    public static Entry poll() {
        Entry entry = TASKS.poll();
        if (entry != null) {
            EntryKey key = new EntryKey(ChunkPos.asLong(entry.pos().x, entry.pos().z), entry.type(), entry.fullClear());
            SCHEDULED.remove(key);
        }
        return entry;
    }

    public static boolean isEmpty() {
        return TASKS.isEmpty();
    }

    public static int size() {
        return TASKS.size();
    }

    public static void clear() {
        TASKS.clear();
        SCHEDULED.clear();
    }

    public record Entry(ChunkPos pos, TaskType type, Season.SubSeason subSeason, boolean fullClear) {
    }

    public enum TaskType {
        APPLY_SNOW,
        MELT_SNOW
    }

    private record EntryKey(long chunkKey, TaskType type, boolean fullClear) {
    }
}

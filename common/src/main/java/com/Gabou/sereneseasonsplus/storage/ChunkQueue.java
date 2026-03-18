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
    private static final Queue<Entry> TASKS_CURRENT_TICK = new ArrayDeque<>();
    private static final Queue<Entry> TASKS_NEXT_TICK = new ArrayDeque<>();

    private static final Set<EntryKey> SCHEDULED = new HashSet<>();


    private static final Queue<Entry> BUGGED_CHUNK  = new ArrayDeque<>();


    private static final Queue<Entry> SCHEDULED_TASKS = new ArrayDeque<>();

    private ChunkQueue() {
    }

    public static void shuffle() {
        TASKS_CURRENT_TICK.addAll(TASKS_NEXT_TICK);
        TASKS_NEXT_TICK.clear();
    }

    public static void enqueueScheduled(ChunkPos chunkPos) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.APPLY_SNOW, true);
        if (SCHEDULED.add(key)) {
            SCHEDULED_TASKS.add(new Entry(chunkPos, TaskType.RETRY, null, true));
        }
    }


    public static void enqueueBugged(ChunkPos chunkPos, Season.SubSeason subSeason) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.APPLY_SNOW, true);
        if (SCHEDULED.add(key)) {
            BUGGED_CHUNK.add(new Entry(chunkPos, TaskType.RETRY, subSeason, true));
        }
    }

    public static int buggedSize() {
        return BUGGED_CHUNK.size();
    }

    public static Entry pollBugged() {
        Entry entry = BUGGED_CHUNK.poll();
        if (entry != null) {
            EntryKey key = new EntryKey(ChunkPos.asLong(entry.pos().x, entry.pos().z), entry.type(), entry.fullClear());
            SCHEDULED.remove(key);
        }
        return entry;
    }

    public static void enqueueApply(ChunkPos chunkPos, Season.SubSeason subSeason) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.APPLY_SNOW, false);
        if (SCHEDULED.add(key)) {
            TASKS_NEXT_TICK.add(new Entry(chunkPos, TaskType.APPLY_SNOW, subSeason, false));
        }
    }

    public static void enqueueMelt(ChunkPos chunkPos, boolean fullClear) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.MELT_SNOW, fullClear);
        if (SCHEDULED.add(key)) {
            TASKS_NEXT_TICK.add(new Entry(chunkPos, TaskType.MELT_SNOW, null, fullClear));
        }
    }

    public static Entry poll() {
        Entry entry = TASKS_CURRENT_TICK.poll();
        if (entry != null) {
            EntryKey key = new EntryKey(ChunkPos.asLong(entry.pos().x, entry.pos().z), entry.type(), entry.fullClear());
            SCHEDULED.remove(key);
        }
        return entry;
    }

    public static boolean isEmpty() {
        return TASKS_CURRENT_TICK.isEmpty();
    }

    public static  boolean isScheduledEmpty() {
        return SCHEDULED_TASKS.isEmpty();
    }


    public static Entry pollScheduled() {
        Entry entry = SCHEDULED_TASKS.poll();
        if (entry != null) {
            EntryKey key = new EntryKey(ChunkPos.asLong(entry.pos().x, entry.pos().z), entry.type(), entry.fullClear());
            SCHEDULED.remove(key);
        }
        return entry;
    }

    public static int size() {
        return TASKS_CURRENT_TICK.size();
    }


    public static int scheduledSize() {
        return SCHEDULED_TASKS.size();
    }

    public static int nextSize() {
        return TASKS_NEXT_TICK.size();
    }

    public static void clear() {
        TASKS_CURRENT_TICK.clear();
        TASKS_NEXT_TICK.clear();
        SCHEDULED.clear();
        SCHEDULED_TASKS.clear();
        BUGGED_CHUNK.clear();
    }

    public record Entry(ChunkPos pos, TaskType type, Season.SubSeason subSeason, boolean fullClear) {
    }

    public enum TaskType {
        APPLY_SNOW,
        MELT_SNOW,
        RETRY
    }

    private record EntryKey(long chunkKey, TaskType type, boolean fullClear) {
    }


}

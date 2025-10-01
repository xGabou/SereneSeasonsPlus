package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Central queue for one-shot chunk-wide snow operations.
 * Tasks are enqueued when a chunk needs an initial snow pass or when
 * a season skip forces a resynchronisation. Entries are removed after
 * a single processing step; there is no incremental work management.
 */
public final class ChunkQueue {
    private static final Queue<Entry> TASKS_CURRENT_TICK = new ArrayDeque<>();
    private static final Queue<Entry> TASKS_NEXT_TICK = new ArrayDeque<>();
    private static final Set<EntryKey> SCHEDULED = new HashSet<>();

    private ChunkQueue() {}

    public static void shuffle() {
        TASKS_CURRENT_TICK.addAll(TASKS_NEXT_TICK);
        TASKS_NEXT_TICK.clear();
    }

    public static void enqueueApply(ChunkPos chunkPos) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.APPLY_SNOW);
        if (SCHEDULED.add(key)) {
            TASKS_NEXT_TICK.add(new Entry(chunkPos, TaskType.APPLY_SNOW));
        }
    }

    public static void enqueueApplyWithSitting(ChunkPos chunkPos, int sittingTicks) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.APPLY_SNOW);
        if (SCHEDULED.add(key)) {
            TASKS_NEXT_TICK.add(new Entry(chunkPos, TaskType.APPLY_SNOW, sittingTicks));
        }
    }

    public static void enqueueMelt(ChunkPos chunkPos) {
        EntryKey key = new EntryKey(ChunkPos.asLong(chunkPos.x, chunkPos.z), TaskType.MELT_SNOW);
        if (SCHEDULED.add(key)) {
            TASKS_NEXT_TICK.add(new Entry(chunkPos, TaskType.MELT_SNOW));
        }
    }

    public static Entry poll() {
        Entry entry = TASKS_CURRENT_TICK.poll();
        if (entry != null) {
            EntryKey key = new EntryKey(ChunkPos.asLong(entry.pos().x, entry.pos().z), entry.type());
            SCHEDULED.remove(key);
        }
        return entry;
    }

    public static boolean isEmpty() {
        return TASKS_CURRENT_TICK.isEmpty();
    }

    public static int size() {
        return TASKS_CURRENT_TICK.size();
    }

    public static int nextSize() {
        return TASKS_NEXT_TICK.size();
    }

    public static void clear() {
        TASKS_CURRENT_TICK.clear();
        TASKS_NEXT_TICK.clear();
        SCHEDULED.clear();
    }

    public record Entry(ChunkPos pos, TaskType type, int sittingTicks) {
        public Entry(ChunkPos pos, TaskType type) {
            this(pos, type, 0);
        }
    }

    public enum TaskType {
        APPLY_SNOW,
        MELT_SNOW
    }

    private record EntryKey(long chunkKey, TaskType type) {}
}

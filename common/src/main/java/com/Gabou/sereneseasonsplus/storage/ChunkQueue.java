package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.Season;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Central queue for one-shot chunk wide snow operations.
 * Tasks are enqueued when a chunk needs an initial snow pass or when
 * a season skip forces a resynchronisation.
 */
public final class ChunkQueue {
    public static final int MAX_DEFER_ATTEMPTS = 100;
    private static final int PROCESSED_COOLDOWN_TICKS = 20;

    private static final Queue<Entry> TASKS_CURRENT_TICK = new ArrayDeque<>();
    private static final Queue<Entry> TASKS_NEXT_TICK = new ArrayDeque<>();
    private static final Set<EntryKey> SCHEDULED = new HashSet<>();
    private static final Map<EntryKey, Integer> COOLDOWN_UNTIL_TICK = new HashMap<>();
    private static final Queue<Entry> BUGGED_CHUNK = new ArrayDeque<>();
    private static final Queue<Entry> SCHEDULED_TASKS = new ArrayDeque<>();
    private static int currentTick = 0;

    private ChunkQueue() {
    }

    public static void setCurrentTick(int tick) {
        currentTick = tick;
        COOLDOWN_UNTIL_TICK.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
    }

    public static void shuffle() {
        TASKS_CURRENT_TICK.addAll(TASKS_NEXT_TICK);
        TASKS_NEXT_TICK.clear();
    }

    public static boolean hasPendingWork() {
        return !TASKS_CURRENT_TICK.isEmpty() || !TASKS_NEXT_TICK.isEmpty();
    }

    public static void enqueueScheduled(ChunkPos chunkPos) {
        Entry entry = new Entry(chunkPos, TaskType.RETRY, null, true, 0);
        if (trySchedule(entry)) {
            SCHEDULED_TASKS.add(entry);
        }
    }

    public static void enqueueBugged(ChunkPos chunkPos, Season.SubSeason subSeason) {
        Entry entry = new Entry(chunkPos, TaskType.RETRY, subSeason, true, 0);
        if (trySchedule(entry)) {
            BUGGED_CHUNK.add(entry);
        }
    }

    public static int buggedSize() {
        return BUGGED_CHUNK.size();
    }

    public static Entry pollBugged() {
        Entry entry = BUGGED_CHUNK.poll();
        if (entry != null) {
            SCHEDULED.remove(keyFor(entry));
        }
        return entry;
    }

    public static void enqueueApply(ChunkPos chunkPos, Season.SubSeason subSeason) {
        Entry entry = new Entry(chunkPos, TaskType.APPLY_SNOW, subSeason, false, 0);
        if (trySchedule(entry)) {
            TASKS_NEXT_TICK.add(entry);
        }
    }

    public static void enqueueMelt(ChunkPos chunkPos, boolean fullClear) {
        Entry entry = new Entry(chunkPos, TaskType.MELT_SNOW, null, fullClear, 0);
        if (trySchedule(entry)) {
            TASKS_NEXT_TICK.add(entry);
        }
    }

    public static void requeueDeferred(Entry entry) {
        Entry next = entry.withAttempts(entry.attempts() + 1);
        if (trySchedule(next, true)) {
            TASKS_NEXT_TICK.add(next);
        }
    }

    public static void deferUntilNextTick(Entry entry) {
        if (trySchedule(entry, true)) {
            TASKS_NEXT_TICK.add(entry);
        }
    }

    public static void markProcessed(Entry entry) {
        COOLDOWN_UNTIL_TICK.put(keyFor(entry), currentTick + PROCESSED_COOLDOWN_TICKS);
    }

    public static void markDropped(Entry entry) {
        COOLDOWN_UNTIL_TICK.put(keyFor(entry), currentTick + PROCESSED_COOLDOWN_TICKS);
    }

    public static Entry poll() {
        Entry entry = TASKS_CURRENT_TICK.poll();
        if (entry != null) {
            SCHEDULED.remove(keyFor(entry));
        }
        return entry;
    }

    public static boolean isEmpty() {
        return TASKS_CURRENT_TICK.isEmpty();
    }

    public static boolean isScheduledEmpty() {
        return SCHEDULED_TASKS.isEmpty();
    }

    public static Entry pollScheduled() {
        Entry entry = SCHEDULED_TASKS.poll();
        if (entry != null) {
            SCHEDULED.remove(keyFor(entry));
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
        COOLDOWN_UNTIL_TICK.clear();
        SCHEDULED_TASKS.clear();
        BUGGED_CHUNK.clear();
        currentTick = 0;
    }

    private static boolean trySchedule(Entry entry) {
        return trySchedule(entry, false);
    }

    private static boolean trySchedule(Entry entry, boolean bypassCooldown) {
        EntryKey key = keyFor(entry);
        if (!bypassCooldown && COOLDOWN_UNTIL_TICK.getOrDefault(key, 0) > currentTick) {
            return false;
        }
        return SCHEDULED.add(key);
    }

    private static EntryKey keyFor(Entry entry) {
        return new EntryKey(ChunkPos.asLong(entry.pos().x, entry.pos().z), entry.type(), entry.fullClear());
    }

    public record Entry(ChunkPos pos, TaskType type, Season.SubSeason subSeason, boolean fullClear, int attempts) {
        public Entry withAttempts(int attempts) {
            return new Entry(pos, type, subSeason, fullClear, attempts);
        }
    }

    public enum TaskType {
        APPLY_SNOW,
        MELT_SNOW,
        RETRY
    }

    private record EntryKey(long chunkKey, TaskType type, boolean fullClear) {
    }
}

package com.Gabou.sereneseasonsplus.features;

public final class SnowProcessingLimits {
    public static final int ACTIVE_SNOW_RANDOM_ATTEMPTS = 64;
    public static final int TIME_BUDGET_MIN_PROCESSED_CHUNKS = 5;
    public static final int MIN_CHUNK_LOAD_RECONCILES_PER_TICK = 4;
    public static final int MAX_CHUNK_LOAD_RECONCILES_PER_TICK = 48;
    public static final int MIN_CHUNK_TASKS_PER_TICK = 3;
    public static final int MAX_CHUNK_TASKS_PER_TICK = 32;
    public static final int MAX_INSPECTED_CHUNK_TASKS_PER_TICK = 96;
    public static final int MIN_MUTATIONS_PER_TICK = 96;
    public static final int MAX_MUTATIONS_PER_TICK = 2048;
    public static final long CHUNK_LOAD_RECONCILE_BUDGET_NANOS = 1_000_000L;
    public static final long CHUNK_TASK_BUDGET_NANOS = 2_000_000L;
    public static final long MUTATION_BUDGET_NANOS = 2_500_000L;
    public static final int DEFAULT_ACTIVE_STORM_TARGET_TICKS = 8000;
    public static final float DEFAULT_STORM_INTENSITY_MULTIPLIER = 1.0f;
    public static final float MIN_STORM_INTENSITY_MULTIPLIER = 0.01f;

    private SnowProcessingLimits() {
    }
}

package com.Gabou.sereneseasonsplus.storage;

/**
 * Central handler for configuration values
 * Values are set once from the config system
 * Other classes should only access through this handler
 */
public class MemoryHandler {

    // Default values (will be overwritten by config)
    private static long forgetTime = 600;
    private static int maxChunksToProcessPerTick =10;
    private static int minChunksToProcessPerTick = 1;
    private static int maxSize = 10000;

    // Private constructor prevents instantiation
    private MemoryHandler() {}

    /** Sets the forget time (ticks or seconds depending on your design) */
    public static void setForgetTime(long value) {
        forgetTime = value;
    }

    /** Gets the forget time */
    public static long getForgetTime() {
        return forgetTime;
    }

    /** Sets max chunks per tick */
    public static void setMaxChunksToProcessPerTick(int value) {
        maxChunksToProcessPerTick = value;
    }

    /** Gets max chunks per tick */
    public static int getMaxChunksToProcessPerTick() {
        return maxChunksToProcessPerTick;
    }

    /** Sets min chunks per tick */
    public static void setMinChunksToProcessPerTick(int value) {
        minChunksToProcessPerTick = value;
    }

    /** Gets min chunks per tick */
    public static int getMinChunksToProcessPerTick() {
        return minChunksToProcessPerTick;
    }

    public static int getMaxSize() {
        return maxSize;
    }
}

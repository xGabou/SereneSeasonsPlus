package com.Gabou.sereneseasonsplus.util;

public class SereneService {

    public static AsyncExecutorHandler HANDLER = new DefaultAsyncExecutorHandler();

    private SereneService() {}

    public static void runAsync(Runnable task) {
        HANDLER.runAsync(task);
    }

    public static void shutdown() {
        HANDLER.shutdown();
    }

    public static void reloadConfig() {
        HANDLER.reloadConfig();
    }
}

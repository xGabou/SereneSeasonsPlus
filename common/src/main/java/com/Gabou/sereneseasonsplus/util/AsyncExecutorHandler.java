package com.Gabou.sereneseasonsplus.util;

public interface AsyncExecutorHandler {
    void runAsync(Runnable task);
    void shutdown();
    void reloadConfig();
}

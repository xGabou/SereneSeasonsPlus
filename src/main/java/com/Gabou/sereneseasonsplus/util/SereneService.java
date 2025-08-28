package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SereneService {

    private static final Logger LOGGER = LogManager.getLogger("SereneService");
    private static boolean initialized = false;
    private static ExecutorService ASYNC_EXECUTOR;
    private static boolean useAsync = SereneExtendedConfig.USE_ASYNC.get();

    private SereneService() { }


    /**
     * Initializes the asynchronous executor service if not already initialized.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        ASYNC_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "useAsync");
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Runs the provided task either asynchronously (if enabled) or on the caller thread.
     *
     * @param task runnable task to execute
     */
    public static void runAsync(Runnable task) {
        if (!useAsync)
            task.run();

        else {
            if(ASYNC_EXECUTOR != null && !ASYNC_EXECUTOR.isShutdown()) {
                ASYNC_EXECUTOR.submit(task);
            }
            else  {
                task.run();
            }
        }
    }

    /**
     * Shuts down the executor service and marks the service uninitialized.
     */
    public static void shutdown() {
        if (ASYNC_EXECUTOR != null) ASYNC_EXECUTOR.shutdown();
        initialized = false;
    }

    /**
     * Reloads configuration-backed flags affecting the service behavior.
     */
    public static void reloadConfig() {
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
}

package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
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

    /**
     * Utility holder; not instantiable.
     */
    private SereneService() {
        
    }


    /**
     * Initializes the single-threaded executor used for background tasks when
     * not delegating to Project Atmosphere. Safe to call more than once.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        ASYNC_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
            LOGGER.info("Creating Async executor");
            Thread t = new Thread(r, "useAsync");
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Executes a task either synchronously, via Project Atmosphere's async
     * weather executor, or on this class' single-thread pool depending on
     * configuration and environment.
     *
     * @param task unit of work to execute
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
     * Shuts down the internal executor, if any.
     */
    public static void shutdown() {
        if (ASYNC_EXECUTOR != null) ASYNC_EXECUTOR.shutdown();
        initialized = false;
    }

    /**
     * Refreshes the cached async usage flag from the config.
     */
    public static void reloadConfig() {
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
}

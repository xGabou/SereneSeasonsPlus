package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.Gabou.projectatmosphere.util.AsyncAtmosphereService;
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
     * Constructs a new instance.
     */
    private SereneService() {
        
    }


    /**
     * TODO: describe method.
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
     * TODO: describe method.
     *
     * @param task description
     */
    public static void runAsync(Runnable task) {
        if (!useAsync)
            task.run();

        else if (SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            AsyncAtmosphereService.runWeather(task);
        }
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
     * TODO: describe method.
     */
    public static void shutdown() {
        if (ASYNC_EXECUTOR != null) ASYNC_EXECUTOR.shutdown();
        initialized = false;
    }

    /**
     * TODO: describe method.
     */
    public static void reloadConfig() {
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
}

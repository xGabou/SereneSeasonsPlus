package com.Gabou.sereneseasonsplus.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultAsyncExecutorHandler implements AsyncExecutorHandler {

    private static final Logger LOGGER = LogManager.getLogger("SereneService");
    private ExecutorService executor;
    protected boolean useAsync =false;

    public DefaultAsyncExecutorHandler() {
        executor = Executors.newSingleThreadExecutor(r -> {
            LOGGER.info("Creating Async executor");
            Thread t = new Thread(r, "useAsync");
            t.setDaemon(false);
            return t;
        });
    }

    @Override
    public void runAsync(Runnable task) {
        if (!useAsync) {
            task.run();
        } else {
            if (executor != null && !executor.isShutdown()) {
                executor.submit(task);
            } else {
                task.run();
            }
        }
    }

    @Override
    public void shutdown() {
        if (executor != null) executor.shutdown();
    }

    @Override
    public void reloadConfig() {
        useAsync = false;
    }


}

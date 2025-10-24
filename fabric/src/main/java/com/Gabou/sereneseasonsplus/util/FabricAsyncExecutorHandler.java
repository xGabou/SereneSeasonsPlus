package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FabricAsyncExecutorHandler extends DefaultAsyncExecutorHandler {

    /**
     * Utility holder; not instantiable.
     */
    public FabricAsyncExecutorHandler()
    {
        super();
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }


    /**
     * Refreshes the cached async usage flag from the config.
     */
    @Override
    public void reloadConfig() {
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
}

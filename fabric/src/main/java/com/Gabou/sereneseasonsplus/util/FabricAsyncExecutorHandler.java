package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;

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

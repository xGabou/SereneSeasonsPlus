package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.Gabou.projectatmosphere.util.AsyncAtmosphereService;

public class NeoForgeAsyncExecutorHandler extends DefaultAsyncExecutorHandler {

    /**
     * Utility holder; not instantiable.
     */
    public NeoForgeAsyncExecutorHandler()
    {
        super();
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
    @Override
    public void runAsync(Runnable task) {
        if (!useAsync) {
            task.run();
        } else if (SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded) {
            AsyncAtmosphereService.runWeather(task);
        } else {
            task.run();
        }
    }


    /**
     * Refreshes the cached async usage flag from the config.
     */
    @Override
    public void reloadConfig() {
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
}

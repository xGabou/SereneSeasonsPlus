package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.Gabou.projectatmosphere.util.AsyncAtmosphereService;

public class ForgeAsyncExecutorHandler extends DefaultAsyncExecutorHandler {



    @Override
    public void runAsync(Runnable task) {
        if (!useAsync) {
            task.run();
        } else if (SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            AsyncAtmosphereService.runWeather(task);
        } else {
            task.run();
        }
    }

    public ForgeAsyncExecutorHandler()
    {
       super();
       useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }

    @Override
    public void reloadConfig() {
        useAsync = SereneExtendedConfig.USE_ASYNC.get();
    }
}

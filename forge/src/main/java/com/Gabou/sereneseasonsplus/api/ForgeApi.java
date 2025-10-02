package com.Gabou.sereneseasonsplus.api;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ProjectAtmosphereRainHandler;
import net.minecraft.server.level.ServerLevel;

public class ForgeApi extends SSAPI {

    public void onSimpleCloudsSpawned(ServerLevel level,int hashCode) {
        EnvironmentHelper.onSimpleCloudSpawned(level,hashCode);
    }

    public void onCloudsDespawned(ServerLevel level,int hashCode) {
        EnvironmentHelper.onSimpleCloudsDespawned(level,hashCode);
    }

}

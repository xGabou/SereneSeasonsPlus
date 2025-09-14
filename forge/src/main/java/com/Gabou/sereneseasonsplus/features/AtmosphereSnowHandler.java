package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.ISnowHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class AtmosphereSnowHandler implements ISnowHandler {
    @Override
    public int getAttemptCount(ServerLevel level, BlockPos center) {
        if (!SereneExtendedConfig.SNOWSTORM_ENABLED.get()) return 0;
        return Math.max(0, SereneExtendedConfig.SNOWSTORM_INTENSITY.get() / 20);
    }
}

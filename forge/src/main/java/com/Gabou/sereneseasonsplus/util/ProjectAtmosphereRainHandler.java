package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Rain handler for Project Atmosphere: use per-position precipitation checks.
 */
public class ProjectAtmosphereRainHandler extends DefaultRainHandler {

    @Override
    protected boolean queryPrecipitation(ServerLevel level, BlockPos pos) {
        try {
            Class<?> api = Class.forName("net.Gabou.projectatmosphere.api.AtmoApi");
            java.lang.reflect.Method method = api.getMethod("isRainingAt", ServerLevel.class, BlockPos.class);
            Object result = method.invoke(null, level, pos);
            if (result instanceof Boolean b) {
                return b;
            }
        } catch (Throwable ignored) {
        }
        return level.isRaining();
    }
}


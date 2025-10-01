package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Rain handler for Project Atmosphere: use per-position precipitation checks.
 */
public class ProjectAtmosphereRainHandler extends DefaultRainHandler {

    @Override
    public boolean isRaining(ServerLevel level, BlockPos pos) {
        return isSnowingSomeWhere(level); // vanilla: global precipitation
    }
    private static boolean isSnowingSomeWhere(ServerLevel level) {
        try {
            Class<?> api = Class.forName("net.Gabou.projectatmosphere.api.AtmoApi");
            java.lang.reflect.Method m = api.getMethod("isSnowingSomeWhere", ServerLevel.class);
            Object res = m.invoke(null, level);
            if (res instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        // Fallback – treat as global rain if reflection failed
        return level.isRaining();
    }
}


package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;

/**
 * Loader-agnostic static proxy. Delegates to the platform-specific impl.
 */
public class EnvironmentHelper {
    private static IEnvironmentHelper delegate;

    /** Called by Fabric/Forge bootstrap to inject the correct impl */
    public static void init(IEnvironmentHelper impl) {
        delegate = impl;
    }

    public static boolean isClient() {
        return delegate.isClient();
    }

    public static boolean isRainning(ServerLevel level, BlockPos pos) {
        return delegate.isRainning(level,pos);
    }

    public static boolean shouldRunMod() {
        return delegate.shouldRunMod();
    }

    public static boolean isHotSeason() {
        return delegate.isHotSeason();
    }

    public static Season.SubSeason getCurrentSeason() {
        return delegate.getCurrentSeason();
    }

    public static void onSeasonChange(ServerLevel serverLevel) {
        delegate.onSeasonChange(serverLevel);
    }
}

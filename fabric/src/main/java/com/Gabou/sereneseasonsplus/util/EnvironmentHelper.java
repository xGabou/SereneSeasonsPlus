package com.Gabou.sereneseasonsplus.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class EnvironmentHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("sereneseasonsplus/EnvironmentHelper");
    private static Season.SubSeason season;
    private static boolean isHotSeason = false;

    /** Returns whether the current cached sub-season counts as a "hot season". */
    public static boolean isHotSeason() {
        return isHotSeason;
    }

    /** Current cached sub-season for the last processed season change event. */
    public static Season.SubSeason getCurrentSeason() {
        return season;
    }



    /** Handles a Serene Seasons change event. */
    public static void onSeasonChange(ServerLevel serverLevel) {
        season = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        LOGGER.info("Season changed to: {}", season);
        isHotSeason = HotSeason.isHotSeason(season);
    }

    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.CLIENT;
    }
}

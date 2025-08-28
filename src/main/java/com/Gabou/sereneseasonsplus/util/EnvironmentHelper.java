




package com.Gabou.sereneseasonsplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class EnvironmentHelper {
    private static final Logger LOGGER = LogManager.getLogger("EnvironmentHelper");
    private static boolean isServerEnvironment;
    private static boolean isSinglePlayer;


    private static Season.SubSeason season;

    private static boolean isHotSeason = false;

    /**
     * TODO: describe method.
     * @return description
     */
    public static boolean isHotSeason() {
        return isHotSeason;
    }

    /**
     * TODO: describe method.
     * @return description
     */
    public static Season.SubSeason getCurrentSeason() {
        return season;
    }

    /**
     * TODO: describe method.
     */
    public static void initialize() {
        isServerEnvironment = !FMLEnvironment.dist.isClient();
        isSinglePlayer = !isServerEnvironment && detectSinglePlayer();
        LOGGER.info("Environment: Server = {}, Single Player = {}", isServerEnvironment, isSinglePlayer);
    }

    /**
     * TODO: describe method.
     * @return description
     */
    public static boolean shouldRunMod() {
        return FMLEnvironment.dist.isDedicatedServer()
                || (FMLEnvironment.dist.isClient() && Minecraft.getInstance().hasSingleplayerServer());
    }


    /**
     * TODO: describe method.
     * @return description
     */
    private static boolean detectSinglePlayer() {
        try {
            Minecraft mcInstance = Minecraft.getInstance();
            return mcInstance.hasSingleplayerServer();
        } catch (Exception e) {
            LOGGER.warn("Failed to determine single-player mode: {}", e.getMessage());
            return false;
        }
    }


    /**
     * TODO: describe method.
     *
     * @param serverLevel description
     */
    public static void onSeasonChange(ServerLevel serverLevel) {
        season = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        LOGGER.info("Season changed to: {}", season);
        isHotSeason = HotSeason.isHotSeason(season);
    }
}

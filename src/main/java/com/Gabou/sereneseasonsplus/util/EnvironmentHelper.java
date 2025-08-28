package com.Gabou.sereneseasonsplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.loading.FMLEnvironment;
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
     * Indicates whether the current sub-season is considered hot.
     *
     * @return true if hot season is active
     */
    public static boolean isHotSeason() {
        return isHotSeason;
    }

    /**
     * Returns the cached current sub-season.
     *
     * @return current sub-season, may be null before first update
     */
    public static Season.SubSeason getCurrentSeason() {
        return season;
    }

    /**
     * Detects environment details and logs basic information.
     */
    public static void initialize() {
        isServerEnvironment = !FMLEnvironment.dist.isClient();
        isSinglePlayer = !isServerEnvironment && detectSinglePlayer();
        LOGGER.info("Environment: Server = {}, Single Player = {}", isServerEnvironment, isSinglePlayer);
    }

    /**
     * Whether the mod logic should run in the current environment.
     *
     * @return true if running on a dedicated server or single-player host
     */
    public static boolean shouldRunMod() {
        return FMLEnvironment.dist.isDedicatedServer()
                || (FMLEnvironment.dist.isClient() && Minecraft.getInstance().hasSingleplayerServer());
    }


    /**
     * Attempts to detect single-player context on the client.
     *
     * @return true if a single-player integrated server is present
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
     * Updates cached season information based on the given server level.
     *
     * @param serverLevel server level where season changed
     */
    public static void onSeasonChange(ServerLevel serverLevel) {
        season = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        LOGGER.info("Season changed to: {}", season);
        isHotSeason = HotSeason.isHotSeason(season);
    }
}

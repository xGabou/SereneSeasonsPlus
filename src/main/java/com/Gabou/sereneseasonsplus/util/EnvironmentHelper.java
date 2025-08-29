




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
     * Returns whether the current cached sub-season counts as a "hot season"
     * for snow behavior purposes.
     *
     * @return true if the active sub-season is hot
     */
    public static boolean isHotSeason() {
        return isHotSeason;
    }

    /**
     * Current cached sub-season for the last processed season change event.
     *
     * @return current sub-season or null if unknown
     */
    public static Season.SubSeason getCurrentSeason() {
        return season;
    }

    /**
     * Detects environment (client/server) and single-player status and logs
     * a summary for debugging.
     */
    public static void initialize() {
        isServerEnvironment = !FMLEnvironment.dist.isClient();
        isSinglePlayer = !isServerEnvironment && detectSinglePlayer();
        LOGGER.info("Environment: Server = {}, Single Player = {}", isServerEnvironment, isSinglePlayer);
    }

    /**
     * Whether seasonal logic should run in the current environment.
     * True on dedicated servers and on clients hosting an integrated server.
     *
     * @return true if mod logic should execute this tick
     */
    public static boolean shouldRunMod() {
        return FMLEnvironment.dist.isDedicatedServer()
                || (FMLEnvironment.dist.isClient() && Minecraft.getInstance().hasSingleplayerServer());
    }


    /**
     * Best-effort detection of single-player (integrated server) mode.
     *
     * @return true if the Minecraft client hosts an integrated server
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
     * Handles a Serene Seasons change event: updates the cached sub-season,
     * logs it, and recomputes the hot-season flag.
     *
     * @param serverLevel the server level where the change occurred
     */
    public static void onSeasonChange(ServerLevel serverLevel) {
        season = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        LOGGER.info("Season changed to: {}", season);
        isHotSeason = HotSeason.isHotSeason(season);
    }
}

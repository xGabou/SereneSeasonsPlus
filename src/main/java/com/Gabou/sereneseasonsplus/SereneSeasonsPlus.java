package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.features.SnowBlockReplacer;
import com.Gabou.sereneseasonsplus.features.SnowPiller;
import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class SereneSeasonsPlus implements ModInitializer {
    public static final String MODID = "sereneseasonsplus";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Serene Seasons Plus (Fabric)");

        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        // Server tick hook
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // If you have client-only stuff, register it in SereneSeasonsPlusClientFabric
    }

    private void onServerStarting(MinecraftServer server) {
        LOGGER.info("Serene Seasons Plus server starting!");
        SereneService.init();
    }

    private void onServerStopping(MinecraftServer server) {
        SereneService.shutdown();
    }

    private void onServerTick(MinecraftServer server) {
        Level level = server.getLevel(Level.OVERWORLD);
        if (level != null) {
            this.onTick(level);
        }
    }

    private void onTick(Level level) {
        if (++this.ticker >= 400) {
            this.ticker = 0;
            if (EnvironmentHelper.isClient()) {
                Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                if (currentSubSeason != this.lastSubSeason) {
                    this.lastSubSeason = currentSubSeason;
                    double daySpeed;
                    double nightSpeed;
                    if (/* seasonal daylight cycle enabled */ false) {
                        daySpeed = getDaySpeedForSeason(currentSubSeason);
                        nightSpeed = getNightSpeedForSeason(currentSubSeason);
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    }
                    // You can add custom config logic here (Fabric config libs like Cloth Config)
                }
            }
        }
    }

    private static void LogInfo(Season.SubSeason currentSubSeason, double daySpeed, double nightSpeed) {
        LOGGER.info("Season: {} → DaySpeed: {}, NightSpeed: {}", currentSubSeason, daySpeed, nightSpeed);
    }

    private double getDaySpeedForSeason(Season.SubSeason season) {
        return switch (season) {
            case EARLY_SPRING -> 1.09;
            case MID_SPRING -> 0.87;
            case LATE_SPRING -> 0.67;
            case EARLY_SUMMER -> 0.59;
            case MID_SUMMER -> 0.67;
            case LATE_SUMMER -> 0.86;
            case EARLY_AUTUMN -> 1.09;
            case MID_AUTUMN -> 1.28;
            case LATE_AUTUMN -> 1.47;
            case EARLY_WINTER -> 1.55;
            case MID_WINTER -> 1.45;
            case LATE_WINTER -> 1.26;
        };
    }

    private double getNightSpeedForSeason(Season.SubSeason season) {
        return switch (season) {
            case EARLY_SPRING -> 0.92;
            case MID_SPRING -> 1.11;
            case LATE_SPRING -> 1.28;
            case EARLY_SUMMER -> 1.35;
            case MID_SUMMER -> 1.28;
            case LATE_SUMMER -> 1.12;
            case EARLY_AUTUMN -> 0.92;
            case MID_AUTUMN -> 0.77;
            case LATE_AUTUMN -> 0.6;
            case EARLY_WINTER -> 0.54;
            case MID_WINTER -> 0.62;
            case LATE_WINTER -> 0.78;
        };
    }
}

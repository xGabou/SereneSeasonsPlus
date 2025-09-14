package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.FabricEnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class SereneSeasonsPlus extends SereneSeasonPlusCommon implements ModInitializer {

    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Serene Seasons Plus (Fabric)");

        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        EnvironmentHelper.init(new FabricEnvironmentHelper());

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


}

package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockReplacer;
import com.Gabou.sereneseasonsplus.features.CommonSnowPiller;
import com.Gabou.sereneseasonsplus.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class SereneSeasonsPlusFabric extends SereneSeasonPlusCommon implements ModInitializer {



    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Serene Seasons Plus (Fabric)");
        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        EnvironmentHelper.init(new FabricEnvironmentHelper());
        SeasonChangeEvent.register();
        SereneExtendedConfig.registerReloadListener(this::onConfigReload);

        // Server tick hook
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommonSnowPiller.init(new VanillaSnowHandler());

        // If you have client-only stuff, register it in SereneSeasonsPlusClientFabric
    }
    private void onConfigReload() {
        CommonSnowBlockReplacer.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get());
    }

    private void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        CommonSnowBlockReplacer.handleOnChunkLoad(chunk);
    }
    private void onServerStarting(MinecraftServer server) {
        LOGGER.info("Serene Seasons Plus server starting!");
        SereneService.HANDLER = new FabricAsyncExecutorHandler();
        CommonSnowBlockReplacer.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get());
        CommonSnowPiller.onServerStarting(SereneExtendedConfig.TICK_SNOW_PILLER.get());
    }

    private void onServerStopping(MinecraftServer server) {
        SereneService.shutdown();
        SereneService.HANDLER = null;
    }

    private void onServerTick(MinecraftServer server) {
        Level level = server.getLevel(Level.OVERWORLD);
        if (level != null) {
            this.onTick(level);
            CommonSnowBlockReplacer.handleServerTick(server);
            CommonSnowPiller.handleServerTick(level);
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
                    if (SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get()) {
                        daySpeed = getDaySpeedForSeason(currentSubSeason);
                        nightSpeed = getNightSpeedForSeason(currentSubSeason);
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    }
                    else if(SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get()) {
                        daySpeed = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
                        nightSpeed = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    }
                    else {
                        LOGGER.info(currentSubSeason.toString()+" is active, but both seasonal and custom daylight cycle are disabled.");
                    }
                    // You can add custom config logic here (Fabric config libs like Cloth Config)
                }
            }
        }
    }


}

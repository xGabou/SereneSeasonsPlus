package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.mixin.MinecraftServerMixin;
import com.Gabou.sereneseasonsplus.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

public class SereneSeasonsPlusFabric extends SereneSeasonPlusCommon implements ModInitializer {


    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Serene Seasons Plus (Fabric)");
        // Server lifecycle hooks
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        EnvironmentHelper.init(new FabricEnvironmentHelper());
        SeasonChangeEvent.register();
        // Register chunk load to cache surface height only (no enqueue)
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        SereneExtendedConfig.registerReloadListener(this::onConfigReload);



        // Server tick hook
        ServerTickEvents.START_WORLD_TICK.register(this::onWorldTick);

        // If you have client-only stuff, register it in SereneSeasonsPlusClientFabric
    }

    private void onConfigReload() {
        CommonSnowBlockFeature.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get(), SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get());
        SereneService.reloadConfig();
    }

    //    private void onChunkLoad(ServerLevel level, LevelChunk chunk) {
//        CommonSnowBlockReplacer.handleOnChunkLoad(chunk);
//    }
    private void onServerStarting(MinecraftServer server) {
        LOGGER.info("Serene Seasons Plus server starting!");
        SereneService.HANDLER = new FabricAsyncExecutorHandler();
        CommonSnowBlockFeature.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get(), SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get());
    }

    private void onServerStopping(MinecraftServer server) {
        SereneService.shutdown();
        SereneService.HANDLER = null;
        EnvironmentHelper.onServerStopping(server.getLevel(Level.OVERWORLD));
    }

    private void onWorldTick(ServerLevel level) {
        if( level.dimension() != Level.OVERWORLD) return;
        RealTimeSeasonHelper.sync(level, SereneExtendedConfig.REAL_TIME_CANADIAN_SEASONS.get());
        this.onTick(
                level,
                SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get(),
                SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.get(),
                SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get(),
                SereneExtendedConfig.CUSTOM_DAY_LENGTH.get(),
                SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get()
        );
        CommonSnowBlockFeature.handleServerTick(level.getServer(), level);

    }

    private void onServerStarted(MinecraftServer server) {
        EnvironmentHelper.onServerStarted(server.getLevel(Level.OVERWORLD));
        RealTimeSeasonHelper.sync(server.getLevel(Level.OVERWORLD), SereneExtendedConfig.REAL_TIME_CANADIAN_SEASONS.get());
        // Apply configured maximum snow accumulation (in layers)
        server.getGameRules()
                .getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT)
                .set(SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get(), server);
    }

    private void onChunkLoad(ServerLevel level, ChunkAccess chunkAccess) {
        if (level == null) return;
        if (!(chunkAccess instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        if (level.isClientSide()) return;
        if (level.dimension() != Level.OVERWORLD) return;
        // Cache surface height only; no enqueue to avoid dual input
        CommonSnowBlockFeature.handleOnChunkLoad(chunk);
    }

}

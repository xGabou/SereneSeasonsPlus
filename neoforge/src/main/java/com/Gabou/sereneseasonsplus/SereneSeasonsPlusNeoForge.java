package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.mixin.MinecraftServerInvoker;
import com.Gabou.sereneseasonsplus.util.*;
import glitchcore.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(SereneSeasonsPlusNeoForge.MODID)
public class SereneSeasonsPlusNeoForge extends SereneSeasonPlusCommon {
    /**
     * Constructs the mod entry point, registers event handlers, and config.
     *
     * @param modEventBus the mod event bus
     */
    public SereneSeasonsPlusNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        NeoForge.EVENT_BUS.register(this);
        EnvironmentHelper.init(new NeoForgeEnvironmentHelper());
        modContainer.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);
        SeasonChangeEvent.register();
        modEventBus.addListener((FMLClientSetupEvent event) -> {
            LOGGER.info("Setting up Serene Seasons Plus (Common)");
            clientSetup(event, modLoadingContext);
        });
    }

    @SubscribeEvent
    /**
     * Initializes services when the server is starting.
     *
     * @param event the server starting event
     */
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Serene Seasons Extended is loading!");
        SereneService.HANDLER = new NeoForgeAsyncExecutorHandler();
        event.getServer().getGameRules().getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT).set(999, event.getServer());
        CommonSnowBlockFeature.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get());
    }

    /**
     * Performs client-side setup such as registering config screens.
     *
     * @param event        the client setup event
     * @param modContainer the active mod container
     */
    private void clientSetup(final FMLClientSetupEvent event, ModLoadingContext modContainer) {
        LOGGER.info("Setting up Serene Seasons Plus (Client)");
        SereneSeasonsPlusNeoForgeClient.init(modContainer);
        NeoForge.EVENT_BUS.register(SereneSeasonsPlusNeoForgeClient.class);
    }

    @SubscribeEvent
    /**
     * Shuts down services when the server stops.
     *
     * @param event the server stopping event
     */
    public void onServerStopping(ServerStoppingEvent event) {
        SereneService.HANDLER.shutdown();
        SereneService.HANDLER = null;
        CommonSnowBlockFeature.onServerStopping();
    }

    @SubscribeEvent
    /**
     * Handles server post-tick to periodically update daylight cycle speeds.
     *
     * @param event server post-tick event
     */
    public void onWorldTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() || !event.hasTime())return;
        ServerLevel level = (ServerLevel) event.getLevel();
        if( level.dimension() != Level.OVERWORLD) return;
        this.onTick(level, SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get(), SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get(), SereneExtendedConfig.CUSTOM_DAY_LENGTH.get(), SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get());
        CommonSnowBlockFeature.handleServerTick((MinecraftServerInvoker) level.getServer(), level);

    }

    @SubscribeEvent
    /**
     * Queues chunk processing when chunks load (e.g., as players move),
     * so snow/ice are cleared or accelerated-melted immediately without rejoining.
     */
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        var level = chunk.getLevel();
        if (level == null || level.isClientSide()) return;
        if (level.dimension() != Level.OVERWORLD) return;
        CommonSnowBlockFeature.handleOnChunkLoad(chunk, (ServerLevel) level);
    }


    @SubscribeEvent
    public void onConfigReload(ServerTickEvent.Pre event) {
        CommonSnowBlockFeature.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get());
        SereneService.reloadConfig();
    }


}

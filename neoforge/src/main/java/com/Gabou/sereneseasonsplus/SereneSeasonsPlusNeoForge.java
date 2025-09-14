package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockReplacer;
import com.Gabou.sereneseasonsplus.features.CommonSnowPiller;
import com.Gabou.sereneseasonsplus.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(SereneSeasonsPlusNeoForge.MODID)
public class SereneSeasonsPlusNeoForge extends SereneSeasonPlusCommon {
    /**
     * Constructs the mod entry point, registers event handlers, and config.
     *
     * @param modEventBus the mod event bus
     */
    public SereneSeasonsPlusNeoForge(IEventBus modEventBus) {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        NeoForge.EVENT_BUS.register(this);
        EnvironmentHelper.init(new NeoForgeEnvironmentHelper());
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);
        SeasonChangeEvent.register();
        CommonSnowPiller.init(new VanillaSnowHandler());
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
        CommonSnowBlockReplacer.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get());
        CommonSnowPiller.onServerStarting(SereneExtendedConfig.TICK_SNOW_PILLER.get());
    }

    /**
     * Performs client-side setup such as registering config screens.
     *
     * @param event        the client setup event
     * @param modContainer the active mod container
     */
    private void clientSetup(final FMLClientSetupEvent event, ModLoadingContext modContainer) {
        LOGGER.info("Setting up Serene Seasons Plus (Client)");
        event.enqueueWork(() -> new SereneSeasonsPlusNeoForgeClient(modContainer));
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
    }

    @SubscribeEvent
    /**
     * Handles server post-tick to periodically update daylight cycle speeds.
     *
     * @param event server post-tick event
     */
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server != null) {
            Level level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                this.onTick(level, SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get(), SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get(), SereneExtendedConfig.CUSTOM_DAY_LENGTH.get(), SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get());
                CommonSnowBlockReplacer.handleServerTick(event.getServer());
                CommonSnowPiller.handleServerTick(level);
            }
        }
    }


}

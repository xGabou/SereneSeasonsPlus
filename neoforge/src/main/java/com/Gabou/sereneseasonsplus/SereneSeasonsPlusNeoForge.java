package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.NeoForgeSnowEnvironmentHandler;
import com.Gabou.sereneseasonsplus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(SereneSeasonsPlusNeoForge.MODID)
public class SereneSeasonsPlusNeoForge extends SereneSeasonPlusCommon {

    public static boolean isProjectAtmosphereLoaded = false;
    /**
     * Constructs the mod entry point, registers event handlers, and config.
     *
     * @param modEventBus the mod event bus
     * @param modContainer the active mod container
     */
    public SereneSeasonsPlusNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        CommonSnowBlockFeature.HANDLER = new NeoForgeSnowEnvironmentHandler();
        EnvironmentHelper.init(new NeoForgeEnvironmentHelper());
        modContainer.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);
        if(!isProjectAtmosphereLoaded) {
            SeasonChangeEvent.register();
        }
        if (isProjectAtmosphereLoaded) {
            EnvironmentHelper.initRainHandler(new ProjectAtmosphereRainHandler());
        }
        modEventBus.addListener((FMLClientSetupEvent event) -> {
            LOGGER.info("Setting up Serene Seasons Plus (Common)");
            clientSetup(event, modContainer);
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
        event.getServer().getGameRules()
                .getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT)
                .set(SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get(), event.getServer());
        CommonSnowBlockFeature.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get(), SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get());
    }


    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        EnvironmentHelper.onServerStarted(event.getServer().getLevel(Level.OVERWORLD));
    }

    /**
     * Performs client-side setup such as registering config screens.
     *
     * @param event        the client setup event
     * @param modContainer the active mod container
     */
    private void clientSetup(final FMLClientSetupEvent event, ModContainer modContainer) {
        LOGGER.info("Setting up Serene Seasons Plus (Client)");
        SereneSeasonsPlusNeoForgeClient.init(ModLoadingContext.get());
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
        CommonSnowBlockFeature.handleServerTick(level.getServer(), level);

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
        CommonSnowBlockFeature.handleOnChunkLoad(chunk);
    }


    @SubscribeEvent
    public void onConfigReload(ServerTickEvent.Pre event) {
        CommonSnowBlockFeature.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get(), SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get());
        SereneService.reloadConfig();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DebugCommands.registerTo(event.getDispatcher());
    }

    /**
     * When a player breaks a snow block/layer during an active storm, mark the column as destroyed
     * for this storm so our accumulation logic will not repopulate it until the next storm.
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (!state.is(Blocks.SNOW) && !state.is(Blocks.SNOW_BLOCK)) return;

        com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData sd = com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData.get();
        int activeId = (sd != null) ? sd.currentStormId : 0;
        if (activeId <= 0) return; // only track during active storm

        LevelChunk chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
        if (!(chunk instanceof com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk tracked)) return;

        if (tracked.sereneseasonsplus$getDestroyedStormId() != activeId) {
            tracked.sereneseasonsplus$getDestroyedColumns().clear();
            tracked.sereneseasonsplus$setDestroyedStormId(activeId);
        }
        long xz = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
        tracked.sereneseasonsplus$getDestroyedColumns().add(xz);

        // Remove any tracked snow column entries for this X/Z so sync won't try to re-add this storm
        tracked.sereneseasonsplus$getSnowColumns().keySet().removeIf(p -> p.getX() == pos.getX() && p.getZ() == pos.getZ());
        chunk.setUnsaved(true);
    }

}






package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.*;
import com.Gabou.sereneseasonsplus.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import static com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge.MODID;

@Mod(MODID)
public class SereneSeasonsPlusForge extends SereneSeasonPlusCommon{

    public static boolean isProjectAtmosphereLoaded = false;

    /**
     * Mod bootstrap: registers event handlers, config, and client setup.
     * Registers season change listeners if Project Atmosphere is absent.
     *
     * @param context Forge mod loading context used to hook lifecycle events
     */
    public SereneSeasonsPlusForge(FMLJavaModLoadingContext context) {
        isProjectAtmosphereLoaded = ModList.get().isLoaded("projectatmosphere");
        MinecraftForge.EVENT_BUS.register(this);
        CommonSnowBlockFeature.HANDLER = new ForgeSnowEnvironmentHandler();
        EnvironmentHelper.init(new ForgeEnvironmentHelper());
        if (isProjectAtmosphereLoaded) {
            EnvironmentHelper.initRainHandler(new ProjectAtmosphereRainHandler());
        }
        context.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);
        if(!isProjectAtmosphereLoaded) {
            SeasonChangeEvent.register();
        }
        context.getModEventBus().addListener((FMLClientSetupEvent event) -> {
            LOGGER.info("Setting up Serene Season Plus (Common)");
            clientSetup(event,context);
        });
    }

    /**
     * Fired when the dedicated/server-integrated instance starts.
     * Initializes background services used by async tasks.
     *
     * @param event Forge server starting event
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Serene Seasons Extended is loading!");
        SereneService.HANDLER = new ForgeAsyncExecutorHandler();
        // Apply configured maximum snow accumulation (in layers)
        event.getServer().getGameRules()
                .getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT)
                .set(SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get(), event.getServer());
        CommonSnowBlockFeature.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get());
    }


    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        EnvironmentHelper.onServerStarted(event.getServer().getLevel(Level.OVERWORLD));
    }

    /**
     * Client-only setup. Enqueues registration of client UI such as the
     * config screen.
     *
     * @param event   client setup event
     * @param context mod loading context
     */
    private void clientSetup(final FMLClientSetupEvent event, FMLJavaModLoadingContext context) {
        LOGGER.info("Setting up Serene Season Plus (Client)");
        event.enqueueWork(() -> {
            SereneSeasonsPlusClientForge.init(context);
        });
        MinecraftForge.EVENT_BUS.register(SereneSeasonsPlusClientForge.class);


    }



    /**
     * Fired when the server is stopping. Shuts down background services.
     *
     * @param event Forge server stopping event
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SereneService.HANDLER.shutdown();
        SereneService.HANDLER = null;
        EnvironmentHelper.onServerStopping(event.getServer().getLevel(Level.OVERWORLD));
    }


    /**
     * Handles server post-tick to periodically update daylight cycle speeds.
     *
     * @param event server post-tick event
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END || !event.haveTime())return;
        ServerLevel level = (ServerLevel) event.level;
        if( level.dimension() != Level.OVERWORLD) return;
        this.onTick(level, SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get(), SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get(), SereneExtendedConfig.CUSTOM_DAY_LENGTH.get(), SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get());
        CommonSnowBlockFeature.handleServerTick(level.getServer(), level);

    }

    /**
     * Placeholder: reserved for config reload tick hook if needed.
     * Currently unused.
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public void onConfigReload(TickEvent.ServerTickEvent event) {
        CommonSnowBlockFeature.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get(), SereneExtendedConfig.SNOWSTORM_ENABLED.get());
        SereneService.reloadConfig();
    }


    /**
     * Queues chunk processing when chunks load (e.g., as players move),
     * so snow/ice are cleared or accelerated-melted immediately without rejoining.
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        var level = chunk.getLevel();
        if (level.isClientSide()) return;
        if (level.dimension() != Level.OVERWORLD) return;
        // Cache surface height only; no enqueue to avoid dual input
        CommonSnowBlockFeature.handleOnChunkLoad(chunk);
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

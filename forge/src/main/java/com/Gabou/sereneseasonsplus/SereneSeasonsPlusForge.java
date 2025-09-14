




package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.*;
import com.Gabou.sereneseasonsplus.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

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
        CommonSnowBlockReplacer.HANDLER = new ForgeSnowEnvironmentHandler();
        EnvironmentHelper.init(new ForgeEnvironmentHelper());
        context.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);
        if(!isProjectAtmosphereLoaded) {
            SeasonChangeEvent.register();
            CommonSnowPiller.init(new VanillaSnowHandler());
        }
        else{
            CommonSnowPiller.init(new AtmosphereSnowHandler());
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
        CommonSnowBlockReplacer.onServerStarting(SereneExtendedConfig.TICK_SNOW_REPLACER.get());
        CommonSnowPiller.onServerStarting(SereneExtendedConfig.TICK_SNOW_PILLER.get());
    }

//    /**
//     * Handles snow and ice in chunks when they are loaded based on temperature.
//     * Extremely warm chunks have all snow removed immediately. Borderline warm
//     * chunks have their snow layers reduced and are queued for gradual melting.
//     *
//     * @param event chunk load event
//     */
//    @SubscribeEvent
//    public void onChunkLoad(ChunkEvent.Load event) {
//        if (!(event.getChunk() instanceof LevelChunk chunk)) {
//            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: non-LevelChunk, skipping");
//            return;
//        }
//
//        Level level = (Level) event.getLevel();
//        if (level.isClientSide()) {
//            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: client side, skipping");
//            return;
//        }
//
//        CommonSnowBlockReplacer.handleOnChunkLoad(chunk);
//    }

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
    }

    /**
     * Server tick hook. At phase END we evaluate whether to run the periodic
     * seasonal update (handled by {@link #onTick(Level)}).
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END) {
            MinecraftServer server = event.getServer();
            if (server != null) {
                Level level = server.getLevel(Level.OVERWORLD);
                if (level != null) {
                    this.onTick(level);
                    CommonSnowBlockReplacer.handleServerTick(event.getServer());
                    CommonSnowPiller.handleServerTick(level);
                }
            }
        }

    }

    /**
     * Placeholder: reserved for config reload tick hook if needed.
     * Currently unused.
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public void onConfigReload(TickEvent.ServerTickEvent event) {
        CommonSnowBlockReplacer.onConfigReload(SereneExtendedConfig.TICK_SNOW_REPLACER.get());
        CommonSnowPiller.onConfigReload(SereneExtendedConfig.TICK_SNOW_PILLER.get());
        SereneService.reloadConfig();
    }

    /**
     * Runs every 400 ticks. If the sub-season changed, updates time-of-day
     * speeds according to configuration (seasonal or custom) and logs the
     * applied values.
     *
     * @param level overworld level reference
     */
    private void onTick(Level level) {
        if (++this.ticker >= 400) {
            this.ticker = 0;
            if (EnvironmentHelper.shouldRunMod()) {
                Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                if (currentSubSeason != this.lastSubSeason ) {
                    this.lastSubSeason = currentSubSeason;
                    if (SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get()) {
                        double daySpeed = this.getDaySpeedForSeason(currentSubSeason);
                        double nightSpeed = this.getNightSpeedForSeason(currentSubSeason);
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    }
                    else if(SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get()) {
                        double daySpeed = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
                        double nightSpeed = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    }
                    else {
                        LOGGER.info(currentSubSeason.toString()+" is active, but both seasonal and custom daylight cycle are disabled.");
                    }
                }


            }
        }
    }


}


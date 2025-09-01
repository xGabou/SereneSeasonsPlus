package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.SnowBlockReplacer;
import com.Gabou.sereneseasonsplus.features.SnowPiller;
import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

@Mod(SereneSeasonsPlus.MODID)
public class SereneSeasonsPlus {
    public static final String MODID = "sereneseasonsplus";
    private static final Logger LOGGER = LogManager.getLogger(SereneSeasonsPlus.class);
    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    /**
     * Constructs the mod entry point, registers event handlers, and config.
     *
     * @param modEventBus the mod event bus
     */
    public SereneSeasonsPlus(IEventBus modEventBus) {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        EnvironmentHelper.initialize();
        NeoForge.EVENT_BUS.register(SnowBlockReplacer.class);
        NeoForge.EVENT_BUS.register(SnowPiller.class);
        NeoForge.EVENT_BUS.register(this);
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);

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
        SereneService.init();
    }

    /**
     * Performs client-side setup such as registering config screens.
     *
     * @param event the client setup event
     * @param modContainer the active mod container
     */
    private void clientSetup(final FMLClientSetupEvent event, ModLoadingContext modContainer) {
        LOGGER.info("Setting up Serene Seasons Plus (Client)");
        event.enqueueWork(() -> new SereneSeasonsPlusClient(modContainer));
    }

    @SubscribeEvent
    /**
     * Shuts down services when the server stops.
     *
     * @param event the server stopping event
     */
    public void onServerStopping(ServerStoppingEvent event) {
        SereneService.shutdown();
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
                this.onTick(level);
            }
        }
    }

    /**
     * Internal tick handler running every few seconds to adjust time speeds
     * according to the current sub-season and configuration.
     *
     * @param level the overworld level
     */
    private void onTick(Level level) {
        if (++this.ticker >= 400) {
            this.ticker = 0;
            if (EnvironmentHelper.shouldRunMod()) {
                Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                if (currentSubSeason != this.lastSubSeason) {
                    this.lastSubSeason = currentSubSeason;
                    if (SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get()) {
                        double daySpeed = this.getDaySpeedForSeason(currentSubSeason);
                        double nightSpeed = this.getNightSpeedForSeason(currentSubSeason);
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    } else if (SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get()) {
                        double daySpeed = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
                        double nightSpeed = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    } else {
                        LOGGER.info(currentSubSeason + " is active, but both seasonal and custom daylight cycle are disabled.");
                    }
                }
            }
        }
    }

    /**
     * Logs the current season and configured day/night speeds.
     *
     * @param currentSubSeason the active sub-season
     * @param daySpeed day speed multiplier
     * @param nightSpeed night speed multiplier
     */
    private static void LogInfo(Season.SubSeason currentSubSeason, double daySpeed, double nightSpeed) {
        LOGGER.info("Season: {} | DaySpeed: {}, NightSpeed: {}", currentSubSeason, daySpeed, nightSpeed);
    }

    /**
     * Returns the day speed for the given sub-season.
     *
     * @param season sub-season
     * @return day speed multiplier
     */
    private double getDaySpeedForSeason(Season.SubSeason season) {
        double v;
        switch (season) {
            case EARLY_SPRING -> v = 1.09;
            case MID_SPRING -> v = 0.87;
            case LATE_SPRING -> v = 0.67;
            case EARLY_SUMMER -> v = 0.59;
            case MID_SUMMER -> v = 0.67;
            case LATE_SUMMER -> v = 0.86;
            case EARLY_AUTUMN -> v = 1.09;
            case MID_AUTUMN -> v = 1.28;
            case LATE_AUTUMN -> v = 1.47;
            case EARLY_WINTER -> v = 1.55;
            case MID_WINTER -> v = 1.45;
            case LATE_WINTER -> v = 1.26;
            default -> throw new IncompatibleClassChangeError();
        }
        return v;
    }

    /**
     * Returns the night speed for the given sub-season.
     *
     * @param season sub-season
     * @return night speed multiplier
     */
    private double getNightSpeedForSeason(Season.SubSeason season) {
        double v;
        switch (season) {
            case EARLY_SPRING -> v = 0.92;
            case MID_SPRING -> v = 1.11;
            case LATE_SPRING -> v = 1.28;
            case EARLY_SUMMER -> v = 1.35;
            case MID_SUMMER -> v = 1.28;
            case LATE_SUMMER -> v = 1.12;
            case EARLY_AUTUMN -> v = 0.92;
            case MID_AUTUMN -> v = 0.77;
            case LATE_AUTUMN -> v = 0.6;
            case EARLY_WINTER -> v = 0.54;
            case MID_WINTER -> v = 0.62;
            case LATE_WINTER -> v = 0.78;
            default -> throw new IncompatibleClassChangeError();
        }
        return v;
    }
}

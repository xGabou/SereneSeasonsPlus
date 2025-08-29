




package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.event.SeasonChangeEvent;
import com.Gabou.sereneseasonsplus.features.SnowBlockReplacer;
import com.Gabou.sereneseasonsplus.features.SnowPiller;
import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import net.Gabou.projectatmosphere.event.SeasonTracker;
import net.Gabou.projectatmosphere.registry.ClientOnlyRegistrar;
import net.minecraft.locale.Language;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.Map;

import static com.Gabou.sereneseasonsplus.SereneSeasonsPlus.MODID;

@Mod(MODID)
public class SereneSeasonsPlus {
    public static final String MODID = "sereneseasonsplus";
    private static final Logger LOGGER = LogManager.getLogger(SereneSeasonsPlus.class);
    public static boolean isProjectAtmosphereLoaded = false;
    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    /**
     * Mod bootstrap: registers event handlers, config, and client setup.
     * Registers season change listeners if Project Atmosphere is absent.
     *
     * @param context Forge mod loading context used to hook lifecycle events
     */
    public SereneSeasonsPlus(FMLJavaModLoadingContext context) {
        isProjectAtmosphereLoaded = ModList.get().isLoaded("projectatmosphere");
        EnvironmentHelper.initialize();
        MinecraftForge.EVENT_BUS.register(SnowBlockReplacer.class);
        MinecraftForge.EVENT_BUS.register(SnowPiller.class);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);
        if(!isProjectAtmosphereLoaded) {
            MinecraftForge.EVENT_BUS.register(SeasonChangeEvent.class);
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
        SereneService.init();
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
            SereneSeasonsPlusClient.init(context);
        });

    }


    /**
     * Fired when the server is stopping. Shuts down background services.
     *
     * @param event Forge server stopping event
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SereneService.shutdown();
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

    /**
     * Logs the active sub-season and the computed day/night speed multipliers.
     *
     * @param currentSubSeason active Serene Seasons sub-season
     * @param daySpeed         day speed multiplier applied
     * @param nightSpeed       night speed multiplier applied
     */
    private static void LogInfo(Season.SubSeason currentSubSeason, double daySpeed, double nightSpeed) {
        LOGGER.info("Season: {} → DaySpeed: {}, NightSpeed: {}", currentSubSeason, daySpeed, nightSpeed);
    }

    /**\n     * Returns the day speed multiplier for the given sub-season. Lower values\n     * make days longer; higher values make them shorter.\n     *\n     * @param season sub-season to evaluate\n     * @return day speed multiplier for that sub-season\n     */
    private double getDaySpeedForSeason(Season.SubSeason season) {
        double var10000;
        switch (season) {
            case EARLY_SPRING -> var10000 = 1.09;
            case MID_SPRING -> var10000 = 0.87;
            case LATE_SPRING -> var10000 = 0.67;
            case EARLY_SUMMER -> var10000 = 0.59;
            case MID_SUMMER -> var10000 = 0.67;
            case LATE_SUMMER -> var10000 = 0.86;
            case EARLY_AUTUMN -> var10000 = 1.09;
            case MID_AUTUMN -> var10000 = 1.28;
            case LATE_AUTUMN -> var10000 = 1.47;
            case EARLY_WINTER -> var10000 = 1.55;
            case MID_WINTER -> var10000 = 1.45;
            case LATE_WINTER -> var10000 = 1.26;
            default -> throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    /**\n     * Returns the day speed multiplier for the given sub-season. Lower values\n     * make days longer; higher values make them shorter.\n     *\n     * @param season sub-season to evaluate\n     * @return day speed multiplier for that sub-season\n     */
    private double getNightSpeedForSeason(Season.SubSeason season) {
        double var10000;
        switch (season) {
            case EARLY_SPRING -> var10000 = 0.92;
            case MID_SPRING -> var10000 = 1.11;
            case LATE_SPRING -> var10000 = 1.28;
            case EARLY_SUMMER -> var10000 = 1.35;
            case MID_SUMMER -> var10000 = 1.28;
            case LATE_SUMMER -> var10000 = 1.12;
            case EARLY_AUTUMN -> var10000 = 0.92;
            case MID_AUTUMN -> var10000 = 0.77;
            case LATE_AUTUMN -> var10000 = 0.6;
            case EARLY_WINTER -> var10000 = 0.54;
            case MID_WINTER -> var10000 = 0.62;
            case LATE_WINTER -> var10000 = 0.78;
            default -> throw new IncompatibleClassChangeError();
        }

        return var10000;
    }
}


//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.features.SnowBlockReplacer;
import com.Gabou.sereneseasonsplus.features.SnowPiller;
import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
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
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import static com.Gabou.sereneseasonsplus.SereneSeasonsPlus.MODID;

@Mod(MODID)
public class SereneSeasonsPlus {
    public static final String MODID = "sereneseasonsplus";
    private static final Logger LOGGER = LogManager.getLogger(SereneSeasonsPlus.class);
    public static boolean isProjectAtmosphereLoaded = false;
    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    public SereneSeasonsPlus(FMLJavaModLoadingContext context) {
        isProjectAtmosphereLoaded = ModList.get().isLoaded("projectatmosphere");
        EnvironmentHelper.initialize();
        MinecraftForge.EVENT_BUS.register(SnowBlockReplacer.class);
        MinecraftForge.EVENT_BUS.register(SnowPiller.class);
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, SereneExtendedConfig.COMMON_SPEC);

        DistExecutor.safeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.Gabou.sereneseasonsplus.SereneSeasonsPlusClient::init);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Serene Seasons Extended is loading!");
        SereneService.init();
    }


    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SereneService.shutdown();
    }

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

    @SubscribeEvent
    public void onConfigReload(TickEvent.ServerTickEvent event) {

    }

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

    private static void LogInfo(Season.SubSeason currentSubSeason, double daySpeed, double nightSpeed) {
        LOGGER.info("Season: {} → DaySpeed: {}, NightSpeed: {}", currentSubSeason, daySpeed, nightSpeed);
    }

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

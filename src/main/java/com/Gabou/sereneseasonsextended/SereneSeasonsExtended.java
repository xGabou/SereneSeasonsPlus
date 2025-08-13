//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.Gabou.sereneseasonsextended;

import com.Gabou.sereneseasonsextended.features.SnowBlockReplacer;
import com.Gabou.sereneseasonsextended.features.SnowPiller;
import com.Gabou.sereneseasonsextended.util.ConfigHacks;
import com.Gabou.sereneseasonsextended.util.EnvironmentHelper;
import betterdays.config.ConfigHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import static com.Gabou.sereneseasonsextended.SereneSeasonsExtended.MODID;

@Mod(MODID)
public class SereneSeasonsExtended {
    public static final String MODID = "sereneseasonsextended";
    private static final Logger LOGGER = LogManager.getLogger(SereneSeasonsExtended.class);
    public static boolean isProjectAtmosphereLoaded = false;
    private int ticker = 0;
    private Season.SubSeason lastSubSeason = null;

    public SereneSeasonsExtended() {
        isProjectAtmosphereLoaded = ModList.get().isLoaded("projectatmosphere");
        EnvironmentHelper.initialize();
            MinecraftForge.EVENT_BUS.register(SnowBlockReplacer.class);
            MinecraftForge.EVENT_BUS.register(SnowPiller.class);
            MinecraftForge.EVENT_BUS.register(this);


    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Serene Seasons Extended is loading!");
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

    private void onTick(Level level) {
        if (++this.ticker >= 400) {
            this.ticker = 0;
            if (EnvironmentHelper.shouldRunMod()) {
                Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                if (currentSubSeason != this.lastSubSeason) {
                    this.lastSubSeason = currentSubSeason;
                    double daySpeed = this.getDaySpeedForSeason(currentSubSeason);
                    double nightSpeed = this.getNightSpeedForSeason(currentSubSeason);
//                    HourglassConfig.SERVER_CONFIG.daySpeed.set(daySpeed);
//                    HourglassConfig.SERVER_CONFIG.nightSpeed.set(nightSpeed);
                    ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                    LOGGER.info("Season: {} → DaySpeed: {}, NightSpeed: {}", currentSubSeason, daySpeed, nightSpeed);
                }

            }
        }
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

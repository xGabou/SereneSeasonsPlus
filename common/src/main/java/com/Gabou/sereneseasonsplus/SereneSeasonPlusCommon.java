package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModTags;

public class SereneSeasonPlusCommon {
    protected int ticker = 0;
    protected Season.SubSeason lastSubSeason = null;
    public static final String MODID = "sereneseasonsplus";
    protected static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /**
     * Logs the active sub-season and the computed day/night speed multipliers.
     *
     * @param currentSubSeason active Serene Seasons sub-season
     * @param daySpeed         day speed multiplier applied
     * @param nightSpeed       night speed multiplier applied
     */
    protected static void LogInfo(Season.SubSeason currentSubSeason, double daySpeed, double nightSpeed) {
        LOGGER.info("Season: {} → DaySpeed: {}, NightSpeed: {}", currentSubSeason, daySpeed, nightSpeed);
    }

    /**
     * Returns the day speed multiplier for the given sub-season. Lower values
     * make days longer; higher values make them shorter.
     * * @param season sub-season to evaluate
     *
     * @return day speed multiplier for that sub-season
     */
    protected double getDaySpeedForSeason(Season.SubSeason season) {
        return switch (season) {
            case EARLY_SPRING -> 1.09;
            case MID_SPRING -> 0.87;
            case LATE_SPRING -> 0.67;
            case EARLY_SUMMER -> 0.59;
            case MID_SUMMER -> 0.67;
            case LATE_SUMMER -> 0.86;
            case EARLY_AUTUMN -> 1.09;
            case MID_AUTUMN -> 1.28;
            case LATE_AUTUMN -> 1.47;
            case EARLY_WINTER -> 1.55;
            case MID_WINTER -> 1.45;
            case LATE_WINTER -> 1.26;
        };
    }

    /**
     * Returns the day speed multiplier for the given sub-season. Lower values
     * make days longer; higher values make them shorter.
     *
     * @param season sub-season to evaluate
     * @return day speed multiplier for that sub-season
     */
    protected double getNightSpeedForSeason(Season.SubSeason season) {
        return switch (season) {
            case EARLY_SPRING -> 0.92;
            case MID_SPRING -> 1.11;
            case LATE_SPRING -> 1.28;
            case EARLY_SUMMER -> 1.35;
            case MID_SUMMER -> 1.28;
            case LATE_SUMMER -> 1.12;
            case EARLY_AUTUMN -> 0.92;
            case MID_AUTUMN -> 0.77;
            case LATE_AUTUMN -> 0.6;
            case EARLY_WINTER -> 0.54;
            case MID_WINTER -> 0.62;
            case LATE_WINTER -> 0.78;
        };
    }

    /**
     * Internal tick handler running every few seconds to adjust time speeds
     * according to the current sub-season and configuration.
     *
     * @param level the overworld level
     */
    protected void onTick(Level level, boolean ENABLE_SEASONAL_DAYLIGHT_CYCLE, boolean CUSTOM_CYCLE_LENGTH, double CUSTOM_DAY_LENGTH, double CUSTOM_NIGHT_LENGTH) {
        if (++this.ticker >= 400) {
            this.ticker = 0;
            if (EnvironmentHelper.shouldRunMod()) {
                Season.SubSeason currentSubSeason = EnvironmentHelper.getCurrentSeason();
                if (currentSubSeason != this.lastSubSeason) {
                    this.lastSubSeason = currentSubSeason;
                    if (ENABLE_SEASONAL_DAYLIGHT_CYCLE) {
                        double daySpeed = this.getDaySpeedForSeason(currentSubSeason);
                        double nightSpeed = this.getNightSpeedForSeason(currentSubSeason);
                        ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                        LogInfo(currentSubSeason, daySpeed, nightSpeed);
                        //level.registryAccess().registryOrThrow(Registries.BIOME).getTagOrEmpty(ModTags.Biomes.BLACKLISTED_BIOMES).forEach((holder -> {LOGGER.info(holder.unwrapKey().get().location().toString());}));
                    } else if (CUSTOM_CYCLE_LENGTH) {
                        ConfigHacks.setTimeSpeeds(CUSTOM_DAY_LENGTH, CUSTOM_NIGHT_LENGTH);
                        LogInfo(currentSubSeason, CUSTOM_DAY_LENGTH, CUSTOM_NIGHT_LENGTH);
                    } else {
                        LOGGER.info(currentSubSeason + " is active, but both seasonal and custom daylight cycle are disabled.");
                    }
                }
            }
        }
    }
}
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
    private double lastAppliedDaySpeed = Double.NaN;
    private double lastAppliedNightSpeed = Double.NaN;
    private String lastTimeMode = "";
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
    protected void onTick(Level level,
                          boolean ENABLE_SEASONAL_DAYLIGHT_CYCLE,
                          boolean ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT,
                          boolean KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH,
                          double FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES,
                          boolean CUSTOM_CYCLE_LENGTH,
                          double CUSTOM_DAY_LENGTH,
                          double CUSTOM_NIGHT_LENGTH) {
        if (++this.ticker >= 400) {
            this.ticker = 0;
            if (EnvironmentHelper.shouldRunMod()) {
                Season.SubSeason currentSubSeason = EnvironmentHelper.getCurrentSeason();
                if (currentSubSeason == null) {
                    return;
                }
                if (!ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT) {
                    return;
                }

                String mode;
                double daySpeed;
                double nightSpeed;
                if (ENABLE_SEASONAL_DAYLIGHT_CYCLE) {
                    mode = "seasonal";
                    double seasonalDaySpeed = this.getDaySpeedForSeason(currentSubSeason);
                    double seasonalNightSpeed = this.getNightSpeedForSeason(currentSubSeason);
                    if (KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH) {
                        ConfigHacks.TimeSpeeds normalized = ConfigHacks.normalizeToCycleMinutes(
                                seasonalDaySpeed, seasonalNightSpeed, FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES);
                        daySpeed = normalized.daySpeed();
                        nightSpeed = normalized.nightSpeed();
                    } else {
                        daySpeed = seasonalDaySpeed;
                        nightSpeed = seasonalNightSpeed;
                    }
                } else if (CUSTOM_CYCLE_LENGTH) {
                    mode = "custom";
                    daySpeed = CUSTOM_DAY_LENGTH;
                    nightSpeed = CUSTOM_NIGHT_LENGTH;
                } else {
                    return;
                }

                if (currentSubSeason != this.lastSubSeason
                        || !mode.equals(this.lastTimeMode)
                        || Double.compare(daySpeed, this.lastAppliedDaySpeed) != 0
                        || Double.compare(nightSpeed, this.lastAppliedNightSpeed) != 0) {
                    ConfigHacks.setTimeSpeeds(daySpeed, nightSpeed);
                    LogInfo(currentSubSeason, daySpeed, nightSpeed);
                    this.lastSubSeason = currentSubSeason;
                    this.lastTimeMode = mode;
                    this.lastAppliedDaySpeed = daySpeed;
                    this.lastAppliedNightSpeed = nightSpeed;
                }
            }
        }
    }
}

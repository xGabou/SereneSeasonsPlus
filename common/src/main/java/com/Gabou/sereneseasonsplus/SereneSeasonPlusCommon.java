package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.util.ConfigHacks;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;

import java.util.EnumMap;
import java.util.Map;

public class SereneSeasonPlusCommon {
    public static final String MODID = "sereneseasonsplus";
    protected static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static final int TIME_SPEED_SYNC_INTERVAL_TICKS = 400;
    public static final Map<Season.SubSeason, TimeSpeeds> DEFAULT_SEASONAL_TIME_SPEEDS = createDefaultSeasonalTimeSpeeds();

    protected int ticker = 0;
    protected Season.SubSeason lastSubSeason = null;
    private TimeSpeeds lastAppliedTimeSpeeds = null;
    private TimeMode lastTimeMode = null;

    /**
     * Current day/night speed pair. Lower values make that part of the day
     * longer; higher values make it shorter.
     */
    public record TimeSpeeds(double daySpeed, double nightSpeed) {
    }

    @FunctionalInterface
    protected interface SeasonalTimeSpeedProvider {
        TimeSpeeds get(Season.SubSeason subSeason);
    }

    private enum TimeMode {
        SEASONAL,
        CUSTOM
    }

    /**
     * Returns the default speed pair for the given sub-season.
     *
     * @param season sub-season to evaluate
     * @return default day/night speed pair for that sub-season
     */
    public static TimeSpeeds getDefaultTimeSpeeds(Season.SubSeason season) {
        return DEFAULT_SEASONAL_TIME_SPEEDS.get(season);
    }

    /**
     * Internal tick handler running every few seconds to adjust Better Days time
     * speeds according to the current sub-season and configuration.
     *
     * @param level the overworld level
     */
    protected void onTick(Level level,
                          boolean enableSeasonalDaylightCycle,
                          boolean enableBetterDaysDynamicTimeCompat,
                          boolean customCycleLength,
                          double customDayLength,
                          double customNightLength,
                          SeasonalTimeSpeedProvider seasonalTimeSpeedProvider) {
        if (++this.ticker < TIME_SPEED_SYNC_INTERVAL_TICKS) {
            return;
        }

        this.ticker = 0;
        if (!EnvironmentHelper.shouldRunMod()) {
            return;
        }

        Season.SubSeason currentSubSeason = EnvironmentHelper.getCurrentSeason();
        if (currentSubSeason == null) {
            return;
        }

        if (!enableBetterDaysDynamicTimeCompat) {
            logStateOnce(currentSubSeason, null, null, "{} is active, but Better Days dynamic time compatibility is disabled.");
            return;
        }

        TimeMode mode;
        TimeSpeeds desiredSpeeds;
        if (enableSeasonalDaylightCycle) {
            mode = TimeMode.SEASONAL;
            desiredSpeeds = seasonalTimeSpeedProvider.get(currentSubSeason);
        } else if (customCycleLength) {
            mode = TimeMode.CUSTOM;
            desiredSpeeds = new TimeSpeeds(customDayLength, customNightLength);
        } else {
            logStateOnce(currentSubSeason, null, null, "{} is active, but both seasonal and custom daylight cycle are disabled.");
            return;
        }

        if (desiredSpeeds == null) {
            LOGGER.warn("No day/night speed config found for {}.", currentSubSeason);
            return;
        }

        if (currentSubSeason == this.lastSubSeason
                && mode == this.lastTimeMode
                && desiredSpeeds.equals(this.lastAppliedTimeSpeeds)) {
            return;
        }

        ConfigHacks.setTimeSpeeds(desiredSpeeds.daySpeed(), desiredSpeeds.nightSpeed());
        logAppliedSpeeds(currentSubSeason, mode, desiredSpeeds);
        this.lastSubSeason = currentSubSeason;
        this.lastTimeMode = mode;
        this.lastAppliedTimeSpeeds = desiredSpeeds;
    }

    private static Map<Season.SubSeason, TimeSpeeds> createDefaultSeasonalTimeSpeeds() {
        EnumMap<Season.SubSeason, TimeSpeeds> speeds = new EnumMap<>(Season.SubSeason.class);
        speeds.put(Season.SubSeason.EARLY_SPRING, new TimeSpeeds(1.09, 0.92));
        speeds.put(Season.SubSeason.MID_SPRING, new TimeSpeeds(0.87, 1.11));
        speeds.put(Season.SubSeason.LATE_SPRING, new TimeSpeeds(0.67, 1.28));
        speeds.put(Season.SubSeason.EARLY_SUMMER, new TimeSpeeds(0.59, 1.35));
        speeds.put(Season.SubSeason.MID_SUMMER, new TimeSpeeds(0.67, 1.28));
        speeds.put(Season.SubSeason.LATE_SUMMER, new TimeSpeeds(0.86, 1.12));
        speeds.put(Season.SubSeason.EARLY_AUTUMN, new TimeSpeeds(1.09, 0.92));
        speeds.put(Season.SubSeason.MID_AUTUMN, new TimeSpeeds(1.28, 0.77));
        speeds.put(Season.SubSeason.LATE_AUTUMN, new TimeSpeeds(1.47, 0.60));
        speeds.put(Season.SubSeason.EARLY_WINTER, new TimeSpeeds(1.55, 0.54));
        speeds.put(Season.SubSeason.MID_WINTER, new TimeSpeeds(1.45, 0.62));
        speeds.put(Season.SubSeason.LATE_WINTER, new TimeSpeeds(1.26, 0.78));
        return Map.copyOf(speeds);
    }

    private void logStateOnce(Season.SubSeason currentSubSeason, TimeMode mode, TimeSpeeds speeds, String message) {
        if (currentSubSeason != this.lastSubSeason || mode != this.lastTimeMode || !sameSpeeds(speeds, this.lastAppliedTimeSpeeds)) {
            LOGGER.info(message, currentSubSeason);
            this.lastSubSeason = currentSubSeason;
            this.lastTimeMode = mode;
            this.lastAppliedTimeSpeeds = speeds;
        }
    }

    private static boolean sameSpeeds(TimeSpeeds first, TimeSpeeds second) {
        return first == null ? second == null : first.equals(second);
    }

    private static void logAppliedSpeeds(Season.SubSeason currentSubSeason, TimeMode mode, TimeSpeeds speeds) {
        LOGGER.info("Season: {} -> Mode: {}, DaySpeed: {}, NightSpeed: {}",
                currentSubSeason, mode, speeds.daySpeed(), speeds.nightSpeed());
    }
}

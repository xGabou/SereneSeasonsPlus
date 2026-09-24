package com.Gabou.sereneseasonsplus.util;

import java.lang.reflect.Field;
import betterdays.config.ConfigHandler;
import betterdays.config.SpeedMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import technology.roughness.whitenoise.config.WhiteNoiseConfigSpec;

public class ConfigHacks {
    private static final double DAY_TICKS = 24000.0;
    private static final double TICKS_PER_REAL_MINUTE = 1200.0;
    private static final Logger LOGGER = LogManager.getLogger("ConfigHacks");
    private static boolean warningLogged;
    private static boolean boundaryWarningLogged;

    public record TimeSpeeds(double daySpeed, double nightSpeed) {
    }

    /**
     * Converts seasonal relative speed weights into Better Days ratios whose
     * day and night durations add up to the requested number of real minutes.
     */
    public static TimeSpeeds normalizeToCycleMinutes(double dayWeight, double nightWeight, double cycleMinutes) {
        if (!Double.isFinite(dayWeight) || dayWeight <= 0.0
                || !Double.isFinite(nightWeight) || nightWeight <= 0.0
                || !Double.isFinite(cycleMinutes) || cycleMinutes <= 0.0) {
            throw new IllegalArgumentException("Time speeds and cycle minutes must be finite and greater than zero");
        }

        double dayTicks = DAY_TICKS / 2.0;
        try {
            Object commonInstance = getCommonInstance();
            double dayStart = getDoubleConfigValue(commonInstance, "dayStart");
            double nightStart = getDoubleConfigValue(commonInstance, "nightStart");
            double configuredDayTicks = (nightStart - dayStart + DAY_TICKS) % DAY_TICKS;
            if (configuredDayTicks > 0.0 && configuredDayTicks < DAY_TICKS) {
                dayTicks = configuredDayTicks;
            }
        } catch (Throwable error) {
            if (!boundaryWarningLogged) {
                boundaryWarningLogged = true;
                LOGGER.warn("Could not read Better Days dayStart/nightStart; using an even 12000/12000 split for cycle-length normalization.", error);
            }
        }

        double dayDurationWeight = 1.0 / dayWeight;
        double nightDurationWeight = 1.0 / nightWeight;
        double weightTotal = dayDurationWeight + nightDurationWeight;
        double dayMinutes = cycleMinutes * dayDurationWeight / weightTotal;
        double nightMinutes = cycleMinutes - dayMinutes;
        double nightTicks = DAY_TICKS - dayTicks;

        return new TimeSpeeds(
                dayTicks / (TICKS_PER_REAL_MINUTE * dayMinutes),
                nightTicks / (TICKS_PER_REAL_MINUTE * nightMinutes)
        );
    }

    /**
     * Overrides BetterDays' time speed configuration using reflection.
     * This adjusts the length of day and night without requiring a restart.
     *
     * @param day   day speed multiplier to set
     * @param night night speed multiplier to set
     */
    public static void setTimeSpeeds(double day, double night) {
        try {
            logCompatibilityWarning();
            Field commonField = ConfigHandler.class.getDeclaredField("COMMON");
            commonField.setAccessible(true);
            setConfigValues(day,night, commonField);
            LOGGER.info("Updated daySpeed = {}, nightSpeed = {}", day, night);
        } catch (Throwable e) {
            LOGGER.error("Failed to set time speeds dynamically", e);
        }
    }

    private static void logCompatibilityWarning() {
        if (warningLogged) {
            return;
        }
        warningLogged = true;
        LOGGER.warn("Better Days dynamic time compatibility is enabled. Serene Seasons Plus will update Better Days daySpeed/nightSpeed using reflection.");
    }

    private static void setConfigValues(double day,double night, Field commonField) throws IllegalAccessException, NoSuchFieldException {
        Object commonInstance = commonField.get(null);

        Field speedMethodField = commonInstance.getClass().getDeclaredField("speedMethod");
        speedMethodField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var speedMethod = (WhiteNoiseConfigSpec.EnumValue<SpeedMethod>) speedMethodField.get(commonInstance);
        speedMethod.set(SpeedMethod.RATIO);

        Field interpolationField = commonInstance.getClass().getDeclaredField("enableInterpolatedTime");
        interpolationField.setAccessible(true);
        var interpolation = (WhiteNoiseConfigSpec.BooleanValue) interpolationField.get(commonInstance);
        interpolation.set(false);

        Field dayField = commonInstance.getClass().getDeclaredField("daySpeed");
        dayField.setAccessible(true);
        var dayValue = (WhiteNoiseConfigSpec.DoubleValue) dayField.get(commonInstance);
        dayValue.set(day);

        Field nightField = commonInstance.getClass().getDeclaredField("nightSpeed");
        nightField.setAccessible(true);
        var nightValue = (WhiteNoiseConfigSpec.DoubleValue) nightField.get(commonInstance);
        nightValue.set(night);
    }

    private static Object getCommonInstance() throws ReflectiveOperationException {
        Field commonField = ConfigHandler.class.getDeclaredField("COMMON");
        commonField.setAccessible(true);
        return commonField.get(null);
    }

    private static double getDoubleConfigValue(Object commonInstance, String fieldName) throws ReflectiveOperationException {
        Field field = commonInstance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        var value = (WhiteNoiseConfigSpec.DoubleValue) field.get(commonInstance);
        return value.get();
    }
}

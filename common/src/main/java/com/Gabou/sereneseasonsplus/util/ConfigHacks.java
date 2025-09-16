package com.Gabou.sereneseasonsplus.util;

import java.lang.reflect.Field;
import betterdays.config.ConfigHandler;
import com.illusivesoulworks.spectrelib.config.SpectreConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHacks {
    private static final Logger LOGGER = LogManager.getLogger("ConfigHacks");

    /**
     * Overrides BetterDays' time speed configuration using reflection.
     * This adjusts the length of day and night without requiring a restart.
     *
     * @param day   day speed multiplier to set
     * @param night night speed multiplier to set
     */
    public static void setTimeSpeeds(double day, double night) {
        try {
            Field commonField = ConfigHandler.class.getDeclaredField("COMMON");
            commonField.setAccessible(true);
            var nightValue = getDoubleValue(day, commonField);
            nightValue.set(night);

            LOGGER.info("Updated daySpeed = {}, nightSpeed = {}", day, night);
        } catch (Exception e) {
            LOGGER.error("Failed to set time speeds dynamically", e);
        }
    }

    private static SpectreConfigSpec.DoubleValue getDoubleValue(double day, Field commonField) throws IllegalAccessException, NoSuchFieldException {
        Object commonInstance = commonField.get(null);

        Field dayField = commonInstance.getClass().getDeclaredField("daySpeed");
        dayField.setAccessible(true);
        var dayValue = (SpectreConfigSpec.DoubleValue) dayField.get(commonInstance);
        dayValue.set(day);

        Field nightField = commonInstance.getClass().getDeclaredField("nightSpeed");
        nightField.setAccessible(true);
        var nightValue = (SpectreConfigSpec.DoubleValue) nightField.get(commonInstance);
        return nightValue;
    }
}

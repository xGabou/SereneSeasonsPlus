package com.Gabou.sereneseasonsextended.util;

import java.lang.reflect.Field;
import betterdays.config.ConfigHandler;
import com.illusivesoulworks.spectrelib.config.SpectreConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHacks {
    private static final Logger LOGGER = LogManager.getLogger("ConfigHacks");

    public static void setTimeSpeeds(double day, double night) {
        try {
            // 🔧 Fix here: setAccessible(true)
            Field commonField = ConfigHandler.class.getDeclaredField("COMMON");
            commonField.setAccessible(true);
            Object commonInstance = commonField.get(null);

            Field dayField = commonInstance.getClass().getDeclaredField("daySpeed");
            dayField.setAccessible(true);
            var dayValue = (SpectreConfigSpec.DoubleValue) dayField.get(commonInstance);
            dayValue.set(day);

            Field nightField = commonInstance.getClass().getDeclaredField("nightSpeed");
            nightField.setAccessible(true);
            var nightValue = (SpectreConfigSpec.DoubleValue) nightField.get(commonInstance);
            nightValue.set(night);

            LOGGER.info("✅ Updated daySpeed = {}, nightSpeed = {}", day, night);
        } catch (Exception e) {
            LOGGER.error("❌ Failed to set time speeds dynamically", e);
        }
    }

}

package com.Gabou.sereneseasonsextended.util;

import java.lang.reflect.Field;
import net.minecraftforge.fml.common.Mod;
import betterdays.config.ConfigHandler;

public class ConfigHacks {

    public static void setTimeSpeeds(double day, double night) {
        try {
            Field dayField = ConfigHandler.Common.class.getDeclaredField("daySpeed");
            dayField.setAccessible(true);
            Object dayValue = dayField.get(null); // static field
            dayValue.getClass().getMethod("set", Object.class).invoke(dayValue, day);

            Field nightField = ConfigHandler.Common.class.getDeclaredField("nightSpeed");
            nightField.setAccessible(true);
            Object nightValue = nightField.get(null); // static field
            nightValue.getClass().getMethod("set", Object.class).invoke(nightValue, night);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

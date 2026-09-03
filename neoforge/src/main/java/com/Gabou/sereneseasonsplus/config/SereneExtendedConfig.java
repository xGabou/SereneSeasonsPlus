package com.Gabou.sereneseasonsplus.config;

import com.Gabou.sereneseasonsplus.SereneSeasonPlusCommon;
import net.neoforged.neoforge.common.ModConfigSpec;
import sereneseasons.api.season.Season;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class SereneExtendedConfig {

    public static final ModConfigSpec.BooleanValue USE_ASYNC;
    public static final ModConfigSpec.IntValue TICK_SNOW_PILLER;
    public static final ModConfigSpec.IntValue TICK_SNOW_REPLACER;
    public static final ModConfigSpec.BooleanValue ENABLE_SEASONAL_DAYLIGHT_CYCLE;
    public static final ModConfigSpec.BooleanValue ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT;
    public static final ModConfigSpec.BooleanValue ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX;
    public static final ModConfigSpec.IntValue BETTER_DAYS_SLEEP_WAKE_TIME;
    public static final ModConfigSpec.DoubleValue CUSTOM_DAY_LENGTH;
    public static final ModConfigSpec.DoubleValue CUSTOM_NIGHT_LENGTH;
    public static final ModConfigSpec.BooleanValue CUSTOM_CYCLE_LENGTH;
    public static final ModConfigSpec.BooleanValue SNOWSTORM_ENABLED;
    public static final ModConfigSpec.IntValue MAX_SNOW_ACCUMULATION_LAYERS;
    public static final ModConfigSpec.BooleanValue GRASS_FLOWER_GROWTH_ENABLED;
    private static final Map<Season.SubSeason, ModConfigSpec.DoubleValue> SEASONAL_DAY_SPEEDS = new EnumMap<>(Season.SubSeason.class);
    private static final Map<Season.SubSeason, ModConfigSpec.DoubleValue> SEASONAL_NIGHT_SPEEDS = new EnumMap<>(Season.SubSeason.class);

    public static final int MIN_CORES_FOR_ASYNC = 6;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("performance");
        USE_ASYNC = builder
                .comment("Use async tasks for some operations to improve performance. This may cause issues with some mods.")
                .define("useAsync", Runtime.getRuntime().availableProcessors()> MIN_CORES_FOR_ASYNC);
        builder.pop();
        builder.push("snowPillerAndReplacer");
        TICK_SNOW_PILLER = builder
                .comment("Tick interval for snow pillers in ticks. Default is 20 (1 second).")
                .defineInRange("tickSnowPiller", 20, 1, Integer.MAX_VALUE);
        TICK_SNOW_REPLACER = builder
                .comment("Tick interval for snow replacer in ticks. Default is 20 (1 second).")
                .defineInRange("tickSnowReplacer", 100, 1, Integer.MAX_VALUE);
        builder.pop();
        builder.push("snowstorm");
        SNOWSTORM_ENABLED = builder
                .comment("Enable snowstorm mode which increases snow pilling intensity.")
                .define("enabled", false);
        builder.pop();
        builder.push("Grass and Flower Growth");
        GRASS_FLOWER_GROWTH_ENABLED = builder
                .comment("Enable enhanced grass and flower growth during warm seasons.")
                .define("enabled", true);
        builder.pop();
        builder.push("snow");
        MAX_SNOW_ACCUMULATION_LAYERS = builder
                .comment("Maximum total snow layers allowed per column (8 layers = 1 block). Default 24 = 3 blocks.")
                .defineInRange("maxSnowAccumulationLayers", 6, 0, 8);
        builder.pop();
        builder.push("seasonalDaylightCycle");
        ENABLE_SEASONAL_DAYLIGHT_CYCLE = builder
                .comment("Enable seasonal daylight cycle. This will change the length of day and night based on the current season.")
                .define("enableSeasonalDaylightCycle", true);
        ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT = builder
                .comment("Allow Serene Seasons Plus to update Better Days daySpeed/nightSpeed at runtime using reflection. Disable this if you want Better Days to fully own time speed behavior.")
                .define("enableBetterDaysDynamicTimeCompat", true);
        ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX = builder
                .comment("When Better Days finishes a sleep cycle, set the wake-up time to betterDaysSleepWakeTime. This does not change Better Days dayStart/nightStart speed boundaries.")
                .define("enableBetterDaysSleepWakeTimeFix", true);
        BETTER_DAYS_SLEEP_WAKE_TIME = builder
                .comment("Minecraft time of day to use after a Better Days sleep cycle. 1000 is 7:00 AM.")
                .defineInRange("betterDaysSleepWakeTime", 1000, 0, 23999);

        CUSTOM_CYCLE_LENGTH = builder
                .comment("If true, the day and night lengths will be determined by the custom values set below. If false, the day and night lengths will be determined by the season.")
                .define("customCycleLength", false);
        CUSTOM_DAY_LENGTH = builder
                .comment("Custom day length in ticks. Only used if seasonal daylight cycle is disabled.")
                .defineInRange("customDayLength", 1, 0.05, 100);
        CUSTOM_NIGHT_LENGTH = builder
                .comment("Custom night length in ticks. Only used if seasonal daylight cycle is disabled.")
                .defineInRange("customNightLength", 1, 0.05, 100);
        builder.push("seasonalSpeeds");
        for (Season.SubSeason subSeason : Season.SubSeason.values()) {
            SereneSeasonPlusCommon.TimeSpeeds defaults = SereneSeasonPlusCommon.getDefaultTimeSpeeds(subSeason);
            String key = subSeason.name().toLowerCase(Locale.ROOT);
            SEASONAL_DAY_SPEEDS.put(subSeason, builder
                    .comment("Day speed multiplier for " + key + ". Lower values make days longer; higher values make them shorter.")
                    .defineInRange(key + "DaySpeed", defaults.daySpeed(), 0.05, 100));
            SEASONAL_NIGHT_SPEEDS.put(subSeason, builder
                    .comment("Night speed multiplier for " + key + ". Lower values make nights longer; higher values make them shorter.")
                    .defineInRange(key + "NightSpeed", defaults.nightSpeed(), 0.05, 100));
        }
        builder.pop();
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public static final ModConfigSpec COMMON_SPEC;

    public static SereneSeasonPlusCommon.TimeSpeeds getSeasonalTimeSpeeds(Season.SubSeason subSeason) {
        ModConfigSpec.DoubleValue daySpeed = SEASONAL_DAY_SPEEDS.get(subSeason);
        ModConfigSpec.DoubleValue nightSpeed = SEASONAL_NIGHT_SPEEDS.get(subSeason);
        if (daySpeed == null || nightSpeed == null) {
            return SereneSeasonPlusCommon.getDefaultTimeSpeeds(subSeason);
        }
        return new SereneSeasonPlusCommon.TimeSpeeds(daySpeed.get(), nightSpeed.get());
    }
}


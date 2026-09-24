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
    public static final ModConfigSpec.BooleanValue KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH;
    public static final ModConfigSpec.DoubleValue FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES;
    public static final ModConfigSpec.DoubleValue CUSTOM_DAY_LENGTH;
    public static final ModConfigSpec.DoubleValue CUSTOM_NIGHT_LENGTH;
    public static final ModConfigSpec.BooleanValue CUSTOM_CYCLE_LENGTH;
    public static final ModConfigSpec.BooleanValue SNOWSTORM_ENABLED;
    public static final ModConfigSpec.IntValue MAX_SNOW_ACCUMULATION_LAYERS;
    public static final ModConfigSpec.BooleanValue GRASS_FLOWER_GROWTH_ENABLED;
    public static final ModConfigSpec.BooleanValue REAL_TIME_CANADIAN_SEASONS;
    private static final Map<Season.SubSeason, ModConfigSpec.DoubleValue> SEASONAL_DAY_SPEEDS = new EnumMap<>(Season.SubSeason.class);
    private static final Map<Season.SubSeason, ModConfigSpec.DoubleValue> SEASONAL_NIGHT_SPEEDS = new EnumMap<>(Season.SubSeason.class);


    public static final int MIN_CORES_FOR_ASYNC = 6;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("performance");
        USE_ASYNC = builder
                .comment("Run supported snow-processing work on background threads. Enabled by default only on systems with more than 6 processor cores. Disable this when diagnosing mod compatibility or threading problems.")
                .define("runSnowUpdatesAsynchronously", Runtime.getRuntime().availableProcessors()> MIN_CORES_FOR_ASYNC);
        builder.pop();
        builder.push("snowUpdateTiming");
        TICK_SNOW_PILLER = builder
                .comment("Number of Minecraft ticks between snow-placement updates. 20 ticks is approximately 1 real-time second.")
                .defineInRange("snowPlacementIntervalInTicks", 20, 1, Integer.MAX_VALUE);
        TICK_SNOW_REPLACER = builder
                .comment("Number of Minecraft ticks between snow replacement and accumulation scans. Lower values update snow more frequently but require more processing. 20 ticks is approximately 1 real-time second.")
                .defineInRange("snowReplacementIntervalInTicks", 100, 1, Integer.MAX_VALUE);
        builder.pop();
        builder.push("seasonalSnowAccumulation");
        SNOWSTORM_ENABLED = builder
                .comment("Allow Serene Seasons Plus to place, replace, and accumulate snow during supported seasonal weather.")
                .define("enableSeasonalSnowAccumulation", false);
        builder.pop();
        builder.push("seasonalPlantGrowth");
        GRASS_FLOWER_GROWTH_ENABLED = builder
                .comment("Allow the mod to apply extra grass and flower growth during warm seasons.")
                .define("enableSeasonalGrassAndFlowerGrowth", true);
        builder.pop();

        builder.push("snowLimits");
        MAX_SNOW_ACCUMULATION_LAYERS = builder
                .comment("Maximum number of snow layers allowed in one vertical column. Eight layers equal one full snow block; the default of 24 equals three blocks.")
                .defineInRange("maximumSnowLayersPerColumn", 24, 0, 512);
        builder.pop();

        builder.push("realWorldSeasonSynchronization");
        REAL_TIME_CANADIAN_SEASONS = builder
                .comment("Synchronize the in-game season with the current real-world date in the America/Toronto time zone. Leave disabled to let Serene Seasons progress normally in-game.")
                .define("syncSeasonsToEasternCanadianCalendar", false);
        builder.pop();

        builder.push("dayNightCycle");
        ENABLE_SEASONAL_DAYLIGHT_CYCLE = builder
                .comment("Change the daylight and nighttime proportions according to the current in-game sub-season, giving summer longer days and winter longer nights.")
                .define("changeDayNightLengthsWithSeasons", true);
        ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT = builder
                .comment("Allow Serene Seasons Plus to control Better Days day and night speed values. Disable this if Better Days or another mod should control time speed by itself.")
                .define("allowSeasonalControlOfBetterDaysTimeSpeed", true);
        KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH = builder
                .comment("Keep one complete day and night at a fixed total elapsed length while seasons change the daylight/night split. When false, the original seasonal speed behavior is preserved. This does not sync to the computer clock.")
                .define("keepFullDayNightCycleAtFixedLength", false);
        FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES = builder
                .comment("Total elapsed real-world minutes in one complete day and night when keepFullDayNightCycleAtFixedLength is enabled. Set to 1440 for 24 hours. This controls duration only and does not sync to the computer clock.")
                .defineInRange("fullDayNightCycleLengthInRealMinutes", 20.0, 0.1, 10080.0);
        ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX = builder
                .comment("When Better Days finishes a sleep cycle, set the wake-up time to betterDaysSleepWakeTime. This does not change Better Days dayStart/nightStart speed boundaries.")
                .define("enableBetterDaysSleepWakeTimeFix", true);
        BETTER_DAYS_SLEEP_WAKE_TIME = builder
                .comment("Minecraft time of day to use after a Better Days sleep cycle. 1000 is 7:00 AM.")
                .defineInRange("betterDaysSleepWakeTime", 1000, 0, 23999);

        CUSTOM_CYCLE_LENGTH = builder
                .comment("Use the custom day and night speed multipliers below when seasonal day/night length changes are disabled.")
                .define("useCustomDayAndNightSpeedMultipliers", false);
        CUSTOM_DAY_LENGTH = builder
                .comment("Better Days speed multiplier for daytime when custom speed mode is active. Values below 1 make daytime longer; values above 1 make it shorter.")
                .defineInRange("customDaySpeedMultiplier", 1, 0.05, 100);
        CUSTOM_NIGHT_LENGTH = builder
                .comment("Better Days speed multiplier for nighttime when custom speed mode is active. Values below 1 make nighttime longer; values above 1 make it shorter.")
                .defineInRange("customNightSpeedMultiplier", 1, 0.05, 100);
        builder.push("seasonalSpeedMultipliers");
        for (Season.SubSeason subSeason : Season.SubSeason.values()) {
            SereneSeasonPlusCommon.TimeSpeeds defaults = SereneSeasonPlusCommon.getDefaultTimeSpeeds(subSeason);
            String key = toConfigPrefix(subSeason);
            String seasonName = subSeason.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            SEASONAL_DAY_SPEEDS.put(subSeason, builder
                    .comment("Daytime speed multiplier during " + seasonName + ". Values below 1 make daytime longer; values above 1 make it shorter.")
                    .defineInRange(key + "DaySpeedMultiplier", defaults.daySpeed(), 0.05, 100));
            SEASONAL_NIGHT_SPEEDS.put(subSeason, builder
                    .comment("Nighttime speed multiplier during " + seasonName + ". Values below 1 make nighttime longer; values above 1 make it shorter.")
                    .defineInRange(key + "NightSpeedMultiplier", defaults.nightSpeed(), 0.05, 100));
        }
        builder.pop();
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public static final ModConfigSpec COMMON_SPEC;

    private static String toConfigPrefix(Season.SubSeason subSeason) {
        String[] words = subSeason.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return result.toString();
    }

    public static SereneSeasonPlusCommon.TimeSpeeds getSeasonalTimeSpeeds(Season.SubSeason subSeason) {
        ModConfigSpec.DoubleValue daySpeed = SEASONAL_DAY_SPEEDS.get(subSeason);
        ModConfigSpec.DoubleValue nightSpeed = SEASONAL_NIGHT_SPEEDS.get(subSeason);
        if (daySpeed == null || nightSpeed == null) {
            return SereneSeasonPlusCommon.getDefaultTimeSpeeds(subSeason);
        }
        return new SereneSeasonPlusCommon.TimeSpeeds(daySpeed.get(), nightSpeed.get());
    }
}

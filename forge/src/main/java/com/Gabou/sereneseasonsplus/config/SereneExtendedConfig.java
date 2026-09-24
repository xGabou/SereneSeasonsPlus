package com.Gabou.sereneseasonsplus.config;

import net.minecraftforge.common.ForgeConfigSpec;


public class SereneExtendedConfig {

    public static final ForgeConfigSpec.BooleanValue USE_ASYNC;
    public static final ForgeConfigSpec.IntValue TICK_SNOW_PILLER;
    public static final ForgeConfigSpec.IntValue TICK_SNOW_REPLACER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SEASONAL_DAYLIGHT_CYCLE;
    public static final ForgeConfigSpec.DoubleValue CUSTOM_DAY_LENGTH;
    public static final ForgeConfigSpec.DoubleValue CUSTOM_NIGHT_LENGTH;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_CYCLE_LENGTH;
    public static final ForgeConfigSpec.BooleanValue SNOWSTORM_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_SNOW_ACCUMULATION_LAYERS;
    public static final ForgeConfigSpec.BooleanValue GRASS_FLOWER_GROWTH_ENABLED;
    public static final ForgeConfigSpec.BooleanValue REAL_TIME_CANADIAN_SEASONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT;
    public static final ForgeConfigSpec.BooleanValue KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH;
    public static final ForgeConfigSpec.DoubleValue FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX;
    public static final ForgeConfigSpec.IntValue BETTER_DAYS_SLEEP_WAKE_TIME;


    public static final int MIN_CORES_FOR_ASYNC = 6;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
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
                .comment("After Better Days finishes accelerating through sleep, move the world to the configured wake time. This does not alter Better Days day-start or night-start boundaries.")
                .define("setWakeTimeAfterBetterDaysSleep", true);
        BETTER_DAYS_SLEEP_WAKE_TIME = builder
                .comment("Minecraft time-of-day tick used after a Better Days sleep cycle. Valid values are 0 through 23999; 1000 is approximately 7:00 AM.")
                .defineInRange("wakeTimeAfterBetterDaysSleepInMinecraftTicks", 1000, 0, 23999);

        CUSTOM_CYCLE_LENGTH = builder
                .comment("Use the custom day and night speed multipliers below when seasonal day/night length changes are disabled.")
                .define("useCustomDayAndNightSpeedMultipliers", false);
        CUSTOM_DAY_LENGTH = builder
                .comment("Better Days speed multiplier for daytime when custom speed mode is active. Values below 1 make daytime longer; values above 1 make it shorter.")
                .defineInRange("customDaySpeedMultiplier", 1, 0.05, 100);
        CUSTOM_NIGHT_LENGTH = builder
                .comment("Better Days speed multiplier for nighttime when custom speed mode is active. Values below 1 make nighttime longer; values above 1 make it shorter.")
                .defineInRange("customNightSpeedMultiplier", 1, 0.05, 100);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public static final ForgeConfigSpec COMMON_SPEC;
}


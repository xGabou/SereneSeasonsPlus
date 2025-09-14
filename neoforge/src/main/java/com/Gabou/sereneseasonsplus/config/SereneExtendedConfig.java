package com.Gabou.sereneseasonsplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;


public class SereneExtendedConfig {

    public static final ModConfigSpec.BooleanValue USE_ASYNC;
    public static final ModConfigSpec.IntValue TICK_SNOW_PILLER;
    public static final ModConfigSpec.IntValue TICK_SNOW_REPLACER;
    public static final ModConfigSpec.BooleanValue ENABLE_SEASONAL_DAYLIGHT_CYCLE;
    public static final ModConfigSpec.DoubleValue CUSTOM_DAY_LENGTH;
    public static final ModConfigSpec.DoubleValue CUSTOM_NIGHT_LENGTH;
    public static final ModConfigSpec.BooleanValue CUSTOM_CYCLE_LENGTH;
    public static final ModConfigSpec.BooleanValue SNOWSTORM_ENABLED;
    public static final ModConfigSpec.IntValue SNOWSTORM_INTENSITY;

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
        SNOWSTORM_INTENSITY = builder
                .comment("Snowstorm intensity value used by external integrations.")
                .defineInRange("intensity", 0, 0, 100);
        builder.pop();
        builder.push("seasonalDaylightCycle");
        ENABLE_SEASONAL_DAYLIGHT_CYCLE = builder
                .comment("Enable seasonal daylight cycle. This will change the length of day and night based on the current season.")
                .define("enableSeasonalDaylightCycle", true);

        CUSTOM_CYCLE_LENGTH = builder
                .comment("If true, the day and night lengths will be determined by the custom values set below. If false, the day and night lengths will be determined by the season.")
                .define("customCycleLength", false);
        CUSTOM_DAY_LENGTH = builder
                .comment("Custom day length in ticks. Only used if seasonal daylight cycle is disabled.")
                .defineInRange("customDayLength", 1, 0.05, 100);
        CUSTOM_NIGHT_LENGTH = builder
                .comment("Custom night length in ticks. Only used if seasonal daylight cycle is disabled.")
                .defineInRange("customNightLength", 1, 0.05, 100);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public static final ModConfigSpec COMMON_SPEC;
}


package com.Gabou.sereneseasonsplus.config;

import com.Gabou.sereneseasonsplus.SereneSeasonPlusCommon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SereneExtendedConfig {
    private static final Logger LOGGER = LogManager.getLogger("SereneExtendedConfig");

    public static final int MIN_CORES_FOR_ASYNC = 6;

    public static final BooleanValue USE_ASYNC;
    public static final IntValue TICK_SNOW_PILLER;
    public static final IntValue TICK_SNOW_REPLACER;
    public static final BooleanValue ENABLE_SEASONAL_DAYLIGHT_CYCLE;
    public static final BooleanValue ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT;
    public static final BooleanValue KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH;
    public static final DoubleValue FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES;
    public static final DoubleValue CUSTOM_DAY_LENGTH;
    public static final DoubleValue CUSTOM_NIGHT_LENGTH;
    public static final BooleanValue CUSTOM_CYCLE_LENGTH;
    public static final BooleanValue SNOWSTORM_ENABLED;
    public static final IntValue MAX_SNOW_ACCUMULATION_LAYERS;
    public static final BooleanValue GRASSFLOWER_GROWTH_ENABLED;
    public static final BooleanValue REAL_TIME_CANADIAN_SEASONS;
    private static final Map<Season.SubSeason, DoubleValue> SEASONAL_DAY_SPEEDS = new EnumMap<>(Season.SubSeason.class);
    private static final Map<Season.SubSeason, DoubleValue> SEASONAL_NIGHT_SPEEDS = new EnumMap<>(Season.SubSeason.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("sereneseasonsplus.json");

    /**
     * listeners called when load() finishes
     */
    private static final List<Runnable> reloadListeners = new ArrayList<>();

    static {
        USE_ASYNC = new BooleanValue("runSnowUpdatesAsynchronously", Runtime.getRuntime().availableProcessors() > MIN_CORES_FOR_ASYNC);

        GRASSFLOWER_GROWTH_ENABLED = new BooleanValue("enableSeasonalGrassAndFlowerGrowth", true);

        TICK_SNOW_PILLER = new IntValue("snowPlacementIntervalInTicks", 20, 1, Integer.MAX_VALUE);
        TICK_SNOW_REPLACER = new IntValue("snowReplacementIntervalInTicks", 100, 1, Integer.MAX_VALUE);

        SNOWSTORM_ENABLED = new BooleanValue("enableSeasonalSnowAccumulation", false);
        // Maximum total layers allowed per snow column (8 layers = 1 block). Default 24 = 3 blocks.
        MAX_SNOW_ACCUMULATION_LAYERS = new IntValue("maximumSnowLayersPerColumn", 24, 0, 512);
        REAL_TIME_CANADIAN_SEASONS = new BooleanValue("syncSeasonsToEasternCanadianCalendar", false);

        ENABLE_SEASONAL_DAYLIGHT_CYCLE = new BooleanValue("changeDayNightLengthsWithSeasons", true);
        ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT = new BooleanValue("allowSeasonalControlOfBetterDaysTimeSpeed", true);
        KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH = new BooleanValue("keepFullDayNightCycleAtFixedLength", false);
        FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES = new DoubleValue("fullDayNightCycleLengthInRealMinutes", 20.0, 0.1, 10080.0);
        CUSTOM_CYCLE_LENGTH = new BooleanValue("useCustomDayAndNightSpeedMultipliers", false);
        CUSTOM_DAY_LENGTH = new DoubleValue("customDaySpeedMultiplier", 1.0, 0.05, 100.0);
        CUSTOM_NIGHT_LENGTH = new DoubleValue("customNightSpeedMultiplier", 1.0, 0.05, 100.0);

        for (Season.SubSeason subSeason : Season.SubSeason.values()) {
            SereneSeasonPlusCommon.TimeSpeeds defaults = SereneSeasonPlusCommon.getDefaultTimeSpeeds(subSeason);
            String key = toConfigPrefix(subSeason);
            SEASONAL_DAY_SPEEDS.put(subSeason, new DoubleValue(key + "DaySpeedMultiplier", defaults.daySpeed(), 0.05, 100.0));
            SEASONAL_NIGHT_SPEEDS.put(subSeason, new DoubleValue(key + "NightSpeedMultiplier", defaults.nightSpeed(), 0.05, 100.0));
        }

        load();
    }

    /**
     * Call this after load() finishes to notify listeners.
     */
    private static void notifyReloadListeners() {
        for (Runnable r : reloadListeners) {
            try {
                r.run();
            } catch (Throwable t) {
                LOGGER.error("Failed to run config reload listener", t);
            }
        }
    }

    /**
     * Mods can subscribe to config reload events.
     */
    public static void registerReloadListener(Runnable listener) {
        reloadListeners.add(listener);
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            notifyReloadListeners();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj == null) return;
            USE_ASYNC.load(obj);
            TICK_SNOW_PILLER.load(obj);
            TICK_SNOW_REPLACER.load(obj);
            SNOWSTORM_ENABLED.load(obj);
            GRASSFLOWER_GROWTH_ENABLED.load(obj);
            MAX_SNOW_ACCUMULATION_LAYERS.load(obj);
            REAL_TIME_CANADIAN_SEASONS.load(obj);
            ENABLE_SEASONAL_DAYLIGHT_CYCLE.load(obj);
            ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.load(obj);
            KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH.load(obj);
            FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES.load(obj);
            CUSTOM_CYCLE_LENGTH.load(obj);
            CUSTOM_DAY_LENGTH.load(obj);
            CUSTOM_NIGHT_LENGTH.load(obj);
            for (DoubleValue value : SEASONAL_DAY_SPEEDS.values()) value.load(obj);
            for (DoubleValue value : SEASONAL_NIGHT_SPEEDS.values()) value.load(obj);
        } catch (Exception ignored) {
        }
        notifyReloadListeners();
    }

    public static void save() {
        try {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            JsonObject obj = new JsonObject();
            USE_ASYNC.save(obj);
            TICK_SNOW_PILLER.save(obj);
            TICK_SNOW_REPLACER.save(obj);
            SNOWSTORM_ENABLED.save(obj);
            MAX_SNOW_ACCUMULATION_LAYERS.save(obj);
            REAL_TIME_CANADIAN_SEASONS.save(obj);
            ENABLE_SEASONAL_DAYLIGHT_CYCLE.save(obj);
            ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.save(obj);
            KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH.save(obj);
            FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES.save(obj);
            CUSTOM_CYCLE_LENGTH.save(obj);
            CUSTOM_DAY_LENGTH.save(obj);
            CUSTOM_NIGHT_LENGTH.save(obj);
            for (DoubleValue value : SEASONAL_DAY_SPEEDS.values()) value.save(obj);
            for (DoubleValue value : SEASONAL_NIGHT_SPEEDS.values()) value.save(obj);
            obj.add("_descriptions", createDescriptions());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static JsonObject createDescriptions() {
        JsonObject descriptions = new JsonObject();
        descriptions.addProperty("runSnowUpdatesAsynchronously", "Runs supported snow-processing work on background threads. Disable this when diagnosing mod compatibility or threading problems.");
        descriptions.addProperty("enableSeasonalGrassAndFlowerGrowth", "Allows extra grass and flower growth during warm seasons.");
        descriptions.addProperty("snowPlacementIntervalInTicks", "Minecraft ticks between snow-placement updates. 20 ticks is approximately 1 real-time second.");
        descriptions.addProperty("snowReplacementIntervalInTicks", "Minecraft ticks between snow replacement and accumulation scans. Lower values update more frequently but require more processing.");
        descriptions.addProperty("enableSeasonalSnowAccumulation", "Allows the mod to place, replace, and accumulate snow during supported seasonal weather.");
        descriptions.addProperty("maximumSnowLayersPerColumn", "Maximum snow layers in one vertical column. Eight layers equal one full snow block.");
        descriptions.addProperty("syncSeasonsToEasternCanadianCalendar", "Synchronizes the in-game season with the current date in the America/Toronto time zone. Leave disabled for normal in-game season progression.");
        descriptions.addProperty("changeDayNightLengthsWithSeasons", "Changes daylight and nighttime proportions using the current in-game sub-season.");
        descriptions.addProperty("allowSeasonalControlOfBetterDaysTimeSpeed", "Allows Serene Seasons Plus to control Better Days day and night speed values.");
        descriptions.addProperty("keepFullDayNightCycleAtFixedLength", "Keeps the complete day-and-night cycle at the configured elapsed duration while seasons change the daylight/night split. Does not synchronize with the computer clock.");
        descriptions.addProperty("fullDayNightCycleLengthInRealMinutes", "Elapsed real-world minutes for one complete day and night when fixed-length mode is enabled. Use 1440 for 24 hours.");
        descriptions.addProperty("useCustomDayAndNightSpeedMultipliers", "Uses the custom day and night speed multipliers when seasonal day/night length changes are disabled.");
        descriptions.addProperty("customDaySpeedMultiplier", "Better Days daytime speed multiplier. Below 1 makes daytime longer; above 1 makes it shorter.");
        descriptions.addProperty("customNightSpeedMultiplier", "Better Days nighttime speed multiplier. Below 1 makes nighttime longer; above 1 makes it shorter.");
        for (Season.SubSeason subSeason : Season.SubSeason.values()) {
            String key = toConfigPrefix(subSeason);
            String seasonName = subSeason.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            descriptions.addProperty(key + "DaySpeedMultiplier", "Daytime speed multiplier during " + seasonName + ". Below 1 makes daytime longer; above 1 makes it shorter.");
            descriptions.addProperty(key + "NightSpeedMultiplier", "Nighttime speed multiplier during " + seasonName + ". Below 1 makes nighttime longer; above 1 makes it shorter.");
        }
        return descriptions;
    }

    private static String toConfigPrefix(Season.SubSeason subSeason) {
        String[] words = subSeason.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return result.toString();
    }

    public static SereneSeasonPlusCommon.TimeSpeeds getSeasonalTimeSpeeds(Season.SubSeason subSeason) {
        DoubleValue daySpeed = SEASONAL_DAY_SPEEDS.get(subSeason);
        DoubleValue nightSpeed = SEASONAL_NIGHT_SPEEDS.get(subSeason);
        if (daySpeed == null || nightSpeed == null) {
            return SereneSeasonPlusCommon.getDefaultTimeSpeeds(subSeason);
        }
        return new SereneSeasonPlusCommon.TimeSpeeds(daySpeed.get(), nightSpeed.get());
    }

    public static final class BooleanValue {
        private final String key;
        private boolean value;
        private final boolean def;

        public BooleanValue(String key, boolean def) {
            this.key = key;
            this.value = def;
            this.def = def;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean v) {
            this.value = v;
        }

        void load(JsonObject obj) {
            if (obj.has(key)) this.value = obj.get(key).getAsBoolean();
        }

        void save(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static final class IntValue {
        private final String key;
        private int value;
        private final int min;
        private final int max;

        public IntValue(String key, int def, int min, int max) {
            this.key = key;
            this.value = def;
            this.min = min;
            this.max = max;
        }

        public int get() {
            return value;
        }

        public void set(int v) {
            this.value = Math.max(min, Math.min(max, v));
        }

        void load(JsonObject obj) {
            if (obj.has(key)) set(obj.get(key).getAsInt());
        }

        void save(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static final class DoubleValue {
        private final String key;
        private double value;
        private final double min;
        private final double max;

        public DoubleValue(String key, double def, double min, double max) {
            this.key = key;
            this.value = def;
            this.min = min;
            this.max = max;
        }

        public double get() {
            return value;
        }

        public void set(double v) {
            this.value = Math.max(min, Math.min(max, v));
        }

        void load(JsonObject obj) {
            if (obj.has(key)) set(obj.get(key).getAsDouble());
        }

        void save(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }
}

package com.Gabou.sereneseasonsplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SereneExtendedConfig {
    private static final Logger LOGGER = LogManager.getLogger("SereneExtendedConfig");

    public static final int MIN_CORES_FOR_ASYNC = 6;

    public static final BooleanValue USE_ASYNC;
    public static final IntValue TICK_SNOW_PILLER;
    public static final IntValue TICK_SNOW_REPLACER;
    public static final BooleanValue ENABLE_SEASONAL_DAYLIGHT_CYCLE;
    public static final BooleanValue ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT;
    public static final DoubleValue CUSTOM_DAY_LENGTH;
    public static final DoubleValue CUSTOM_NIGHT_LENGTH;
    public static final BooleanValue CUSTOM_CYCLE_LENGTH;
    public static final BooleanValue SNOWSTORM_ENABLED;
    public static final IntValue MAX_SNOW_ACCUMULATION_LAYERS;
    public static final BooleanValue GRASSFLOWER_GROWTH_ENABLED;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("sereneseasonsplus.json");

    /**
     * listeners called when load() finishes
     */
    private static final List<Runnable> reloadListeners = new ArrayList<>();

    static {
        USE_ASYNC = new BooleanValue("useAsync", Runtime.getRuntime().availableProcessors() > MIN_CORES_FOR_ASYNC);

        GRASSFLOWER_GROWTH_ENABLED = new BooleanValue("grassFlowerGrowthEnabled", true);

        TICK_SNOW_PILLER = new IntValue("tickSnowPiller", 20, 1, Integer.MAX_VALUE);
        TICK_SNOW_REPLACER = new IntValue("tickSnowReplacer", 100, 1, Integer.MAX_VALUE);

        SNOWSTORM_ENABLED = new BooleanValue("snowstormEnabled", false);
        // Maximum total layers allowed per snow column (8 layers = 1 block). Default 24 = 3 blocks.
        MAX_SNOW_ACCUMULATION_LAYERS = new IntValue("maxSnowAccumulationLayers", 6, 0, 8);

        ENABLE_SEASONAL_DAYLIGHT_CYCLE = new BooleanValue("enableSeasonalDaylightCycle", true);
        ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT = new BooleanValue("enableBetterDaysDynamicTimeCompat", true);
        CUSTOM_CYCLE_LENGTH = new BooleanValue("customCycleLength", false);
        CUSTOM_DAY_LENGTH = new DoubleValue("customDayLength", 1.0, 0.05, 100.0);
        CUSTOM_NIGHT_LENGTH = new DoubleValue("customNightLength", 1.0, 0.05, 100.0);

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
            GRASSFLOWER_GROWTH_ENABLED.load(obj);
            TICK_SNOW_PILLER.load(obj);
            TICK_SNOW_REPLACER.load(obj);
            SNOWSTORM_ENABLED.load(obj);
            MAX_SNOW_ACCUMULATION_LAYERS.load(obj);
            ENABLE_SEASONAL_DAYLIGHT_CYCLE.load(obj);
            ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.load(obj);
            CUSTOM_CYCLE_LENGTH.load(obj);
            CUSTOM_DAY_LENGTH.load(obj);
            CUSTOM_NIGHT_LENGTH.load(obj);
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
            GRASSFLOWER_GROWTH_ENABLED.save(obj);
            TICK_SNOW_PILLER.save(obj);
            TICK_SNOW_REPLACER.save(obj);
            SNOWSTORM_ENABLED.save(obj);
            MAX_SNOW_ACCUMULATION_LAYERS.save(obj);
            ENABLE_SEASONAL_DAYLIGHT_CYCLE.save(obj);
            ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.save(obj);
            CUSTOM_CYCLE_LENGTH.save(obj);
            CUSTOM_DAY_LENGTH.save(obj);
            CUSTOM_NIGHT_LENGTH.save(obj);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException ignored) {
        }
        notifyReloadListeners();
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


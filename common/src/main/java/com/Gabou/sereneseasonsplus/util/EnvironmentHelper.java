package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.storage.SnowSavedData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import sereneseasons.api.season.Season;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loader-agnostic static proxy. Delegates to the platform-specific impl.
 */
public class EnvironmentHelper {
    private static IEnvironmentHelper delegate;
    private static IRainHandler rainHandler = new DefaultRainHandler();

    /** Called by Fabric/Forge bootstrap to inject the correct impl */
    public static void init(IEnvironmentHelper impl) {
        delegate = impl;
    }

    public static boolean isClient() {
        return delegate.isClient();
    }

    public static boolean isRainning(ServerLevel level, BlockPos pos) {
        return rainHandler.isRainingAt(level, pos);
    }

    public static void onSimpleCloudSpawned(ServerLevel level, int hashcode) {
        rainHandler.onSimpleCloudsSpawned(level,hashcode);
    }

    public static void checkAndUpdate(ServerLevel level) {
        rainHandler.checkAndUpdate(level);
    }

    public static void  onSimpleCloudsDespawned(ServerLevel level,int hashcode) {
        rainHandler.onSimpleCloudsDespawned(level,hashcode);
    }

    /**
     * Allows platform bootstrap to install a custom rain handler.
     */
    public static void initRainHandler(IRainHandler handler) {
        if (handler != null) rainHandler = handler;
    }
    // Persistence: world-level state we want to keep across reloads
    private static final String SAVE_DIR = "data/sereneseasonsplus";
    private static final String SAVE_FILE = "world_state.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Track last seen sub-season to avoid double-incrementing winter ID on restart
    private static Season.SubSeason lastSubSeason = null;

    // Load persisted data when a world starts
    public static void onWorldLoad(ServerLevel level) {
        if (level == null || level.isClientSide()) return;
        Path path = getWorldDataPath(level);
        try {
            if (Files.exists(path)) {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                JsonObject obj = GSON.fromJson(raw, JsonObject.class);
                if (obj != null) {
                    if (obj.has("currentWinterId")) {
                        currentWinterId = obj.get("currentWinterId").getAsInt();
                    }
                    if (obj.has("lastSubSeason")) {
                        try {
                            lastSubSeason = Season.SubSeason.valueOf(obj.get("lastSubSeason").getAsString());
                        } catch (IllegalArgumentException ignored) {
                            lastSubSeason = null;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // If anything goes wrong, continue with defaults
        }
    }

    // Save persisted data when a world stops
    public static void onWorldSave(ServerLevel level) {
        if (level == null || level.isClientSide()) return;
        Path path = getWorldDataPath(level);
        try {
            Files.createDirectories(path.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("version", 1);
            obj.addProperty("currentWinterId", currentWinterId);
            Season.SubSeason cur = lastSubSeason != null ? lastSubSeason : getCurrentSeason();
            if (cur != null) obj.addProperty("lastSubSeason", cur.name());
            String json = GSON.toJson(obj);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path getWorldDataPath(ServerLevel level) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        return root.resolve(SAVE_DIR).resolve(SAVE_FILE);
    }

    public static boolean shouldRunMod() {
        return delegate.shouldRunMod();
    }

    public static boolean isHotSeason() {
        return delegate.isHotSeason();
    }

    public static boolean isSnowySeason() {
        return delegate.isSnowySeason();
    }

    public static Season.SubSeason getCurrentSeason() {
        return delegate.getCurrentSeason();
    }

    public static void onServerStarted(ServerLevel level)
    {
        // Store overworld reference for no-level accessors
        WorldContext.setOverworld(level);
        onWorldLoad(level);
        onSeasonChange(level,false);
        CommonSnowBlockFeature.HANDLER.resetWinterState(level, currentWinterId);
    }


    private static int currentWinterId = 0;

    public static int getCurrentWinterId() {
        return currentWinterId;
    }

    public static void onSeasonChange(ServerLevel serverLevel, boolean forced) {
        delegate.onSeasonChange(serverLevel);

        // Detect first sub-season of winter, avoiding double-count on reload by comparing last sub-season
        Season.SubSeason current = getCurrentSeason();
        if (current == Season.SubSeason.EARLY_WINTER && lastSubSeason != Season.SubSeason.EARLY_WINTER) {
            currentWinterId++; // new winter!
            CommonSnowBlockFeature.HANDLER.resetWinterState(serverLevel, currentWinterId);
        }
        lastSubSeason = current;

        // If we enter a hot season, reset snow history so future winters don't inherit it
        if (HotSeason.isHotSeason(current)) {
            SnowHistorySavedData hist = SnowHistorySavedData.get();
            hist.currentStormId = 0;
            hist.snowHistory.clear();
            hist.setDirty();
        }

        if (forced) {
            CommonSnowBlockFeature.onSeasonChange(serverLevel);
        }
    }

    public static void onServerStopping(ServerLevel level) {
        onWorldSave(level);
        CommonSnowBlockFeature.onServerStopping();
        CommonSnowBlockFeature.HANDLER.clear(level);
        // Clear overworld reference
        WorldContext.clear();
        // Drop cached singletons bound to the previous world
        SnowSavedData.clearCachedInstance();
        SnowHistorySavedData.clearCachedInstance();
    }
    public static int getGrassChance() {
        return delegate.getGrassChance(false);
    }

}

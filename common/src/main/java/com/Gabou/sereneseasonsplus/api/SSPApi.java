package com.Gabou.sereneseasonsplus.api;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import net.Gabou.gaboulibs.storage.SnowRecord;
import com.Gabou.sereneseasonsplus.storage.SnowSavedData;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;

/**
 * Public API for tuning SereneSeasonsPlus snow storm behavior at runtime.
 *
 * Exposes storm piling speed and intensity, and allows updating the
 * active storm's min/avg/max layer parameters while a storm is in progress.
 */
public final class SSPApi {

    private static SSPApi INSTANCE = new SSPApi();

    public static final String MODID = "sereneseasonsplus";


    public static SSPApi getINSTANCE() {
        return INSTANCE;
    }

    private SSPApi() {}

    // -------- Piling Speed Controls --------

    /**
     * Enables or disables fast piling mode. When enabled, chunks jump directly
     * to the full storm distribution instead of gradually approaching it.
     */
    public static void setFastPilingMode(boolean enabled) {
        CommonSnowBlockFeature.setFastPilingMode(enabled);
    }

    /** Returns whether fast piling mode is enabled. */
    public static boolean isFastPilingMode() {
        return CommonSnowBlockFeature.FAST_PILING_MODE;
    }

    /**
     * Sets the target number of ticks to reach full storm distribution per chunk
     * when fast piling is disabled.
     */
    public static void setStormTargetTicks(int ticks) {
        CommonSnowBlockFeature.setActiveStormTargetTicks(ticks);
    }

    /** Returns the current storm target ticks. */
    public static int getStormTargetTicks() {
        return CommonSnowBlockFeature.ACTIVE_STORM_TARGET_TICKS;
    }

    /**
     * Globally scales the piling speed during active storms. Values > 1 speed up, < 1 slow down.
     */
    public static void setStormIntensityMultiplier(float multiplier) {
        CommonSnowBlockFeature.setStormIntensityMultiplier(multiplier);
    }

    /** Returns the current storm intensity multiplier. */
    public static float getStormIntensityMultiplier() {
        return CommonSnowBlockFeature.STORM_INTENSITY_MULTIPLIER;
    }

    // -------- Active Storm Parameters (min/avg/max) --------

    /**
     * Updates the active storm's min/avg/max layer targets.
     * Returns false if there is no active storm on this level.
     */
    public static boolean setCurrentStormLayers(ServerLevel level, float min, float avg, float max) {
        if (level == null || level.isClientSide()) return false;
        SnowHistorySavedData sd = SnowHistorySavedData.get();
        if (sd == null || sd.currentStormId <= 0) return false;

        int id = sd.currentStormId;
        SnowRecord rec = sd.snowHistory.get(id);
        if (rec == null) {
            rec = new SnowRecord();
            sd.snowHistory.put(id, rec);
        }

        // Clamp values to sane ranges and relationships
        float clampedMin = Math.max(0f, min);
        float clampedMax = Math.max(clampedMin, max);
        float clampedAvg = Math.max(clampedMin, Math.min(clampedMax, avg));

        rec.minLayers = clampedMin;
        rec.avgLayers = clampedAvg;
        rec.maxLayers = clampedMax;
        sd.setDirty();
        return true;
    }

    /** Sets only the min layer target for the active storm. */
    public static boolean setCurrentStormMin(ServerLevel level, float min) {
        SnowRecord rec = getOrCreateActiveRecord(level);
        if (rec == null) return false;
        float clampedMin = Math.max(0f, min);
        rec.minLayers = clampedMin;
        if (rec.maxLayers < clampedMin) rec.maxLayers = clampedMin;
        if (rec.avgLayers < clampedMin) rec.avgLayers = clampedMin;
        SnowHistorySavedData.get().setDirty();
        return true;
    }

    /** Sets only the avg layer target for the active storm. */
    public static boolean setCurrentStormAvg(ServerLevel level, float avg) {
        SnowRecord rec = getOrCreateActiveRecord(level);
        if (rec == null) return false;
        float clamped = Math.max(rec.minLayers, Math.min(rec.maxLayers, avg));
        rec.avgLayers = clamped;
        SnowHistorySavedData.get().setDirty();
        return true;
    }

    /** Sets only the max layer target for the active storm. */
    public static boolean setCurrentStormMax(ServerLevel level, float max) {
        SnowRecord rec = getOrCreateActiveRecord(level);
        if (rec == null) return false;
        float clamped = Math.max(rec.minLayers, max);
        rec.maxLayers = clamped;
        if (rec.avgLayers > rec.maxLayers) rec.avgLayers = rec.maxLayers;
        SnowHistorySavedData.get().setDirty();
        return true;
    }

    /** Returns the active storm's SnowRecord, or null if none is active. */
    public static SnowRecord getCurrentStormRecord(ServerLevel level) {
        if (level == null || level.isClientSide()) return null;
        SnowHistorySavedData sd = SnowHistorySavedData.get();
        if (sd == null || sd.currentStormId <= 0) return null;
        return sd.snowHistory.get(sd.currentStormId);
    }

    // Helpers
    private static SnowRecord getOrCreateActiveRecord(ServerLevel level) {
        if (level == null || level.isClientSide()) return null;
        SnowHistorySavedData sd = SnowHistorySavedData.get();
        if (sd == null || sd.currentStormId <= 0) return null;
        SnowRecord rec = sd.snowHistory.get(sd.currentStormId);
        if (rec == null) {
            rec = new SnowRecord();
            sd.snowHistory.put(sd.currentStormId, rec);
        }
        return rec;
    }


    public void onSimpleCloudsSpawned(ServerLevel level,int hashCode) {
        EnvironmentHelper.onSimpleCloudSpawned(level,hashCode);
    }

    public void onCloudsDespawned(ServerLevel level,int hashCode) {
        EnvironmentHelper.onSimpleCloudsDespawned(level,hashCode);
    }

    // -------- Reset Utilities --------

    /** Clears the active storm flag and history records for this level. */
    public static void resetSnowHistory(ServerLevel level) {
        if (level == null || level.isClientSide()) return;
        SnowHistorySavedData hist = SnowHistorySavedData.get();
        hist.currentStormId = 0;
        hist.snowHistory.clear();
        hist.setDirty();
    }

    /**
     * Resets persisted environment counters and flags (storm count, active flag,
     * observed/pending sets). Does not change winter id.
     */
    public static void resetEnvironmentState(ServerLevel level) {
        if (level == null || level.isClientSide()) return;
        SnowSavedData env = SnowSavedData.get();
        env.stormCount = 0;
        env.stormActive = false;
        env.pendingChunks.clear();
        env.observedChunks.clear();
        env.lastBlanketStormCount = 0;
        env.setDirty();
        // Clear handler cache so it reloads from SnowSavedData
        CommonSnowBlockFeature.HANDLER.clear(level);
    }

    /** Resets progress tracking for a specific loaded chunk, if present. */
    public static void resetChunkProgress(ServerLevel level, int chunkX, int chunkZ) {
        if (level == null || level.isClientSide()) return;
        var chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
        if (chunk instanceof ISnowTrackedChunk tracked) {
            tracked.sereneseasonsplus$setStormProgress(0f);
            tracked.sereneseasonsplus$setStormIdApplied(0);
            tracked.sereneseasonsplus$setLastProgressTick(0);
        }
    }

    /**
     * Resets storm progress for chunks around all players within view distance.
     */
    public static void resetLoadedChunkProgress(ServerLevel level) {
        if (level == null || level.isClientSide()) return;
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            int view = player.getServer() != null ? player.getServer().getPlayerList().getViewDistance() : 10;
            int radius = Math.max(1, view);
            int baseCX = player.blockPosition().getX() >> 4;
            int baseCZ = player.blockPosition().getZ() >> 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    resetChunkProgress(level, baseCX + dx, baseCZ + dz);
                }
            }
        }
    }

    /** Convenience: clears history, environment counters and loaded chunk progress. */
    public static void resetAllStormData(ServerLevel level) {
        resetSnowHistory(level);
        resetEnvironmentState(level);
        resetLoadedChunkProgress(level);
    }
}

package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.storage.SnowRecord;
import com.Gabou.sereneseasonsplus.storage.SnowSavedData;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SnowGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import sereneseasons.season.SeasonHooks;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles snow lifecycle for vanilla/Serene Seasons.
 * Supports only one global storm at a time (tracked as ID 0).
 */
public class DefaultSnowEnvironmentHandler implements ISnowEnvironmentHandler {

    protected static final class SnowData {
        int winterId = -1;
        int stormCount = 0;
        final Set<Integer> activeStorms = new HashSet<>(); // active storm IDs
    }

    protected SnowData overworldData;

    protected boolean isOverworld(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    protected SnowData getOrCreateData(ServerLevel level) {
        if (overworldData == null) {
            overworldData = new SnowData();
            restore(level, overworldData);
        }
        return overworldData;
    }

    protected void restore(ServerLevel level, SnowData data) {
        SnowSavedData store = SnowSavedData.get();
        data.winterId = store.winterId;
        data.stormCount = store.stormCount;
        data.activeStorms.clear(); // only restored count, not sessions
    }

    protected void persist(ServerLevel level, SnowData data) {
        SnowSavedData store = SnowSavedData.get();
        store.winterId = data.winterId;
        store.stormCount = data.stormCount;
        store.setDirty();
    }

    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos pos) {
        if (!isOverworld(level)) return 0;

        float temperature = SeasonHooks.getBiomeTemperature(level, level.getBiome(pos), pos);
        boolean cold = SeasonHooks.coldEnoughToSnowSeasonal(level, pos);
        boolean precip = EnvironmentHelper.isRainning(level, pos);
        // Place snow flurries when cold and precipitating (signal with -1)
        if (cold && precip) return -1;
        // Remove snow only when it is NOT cold enough
        return cold ? 0 : CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
    }

    @Override
    public void resetWinterState(ServerLevel level, int winterId) {
        if (!isOverworld(level)) return;

        SnowData data = getOrCreateData(level);
        if (data.winterId == winterId) return;

        data.winterId = winterId;
        data.stormCount = 0;
        data.activeStorms.clear();
        persist(level, data);

        SnowHistorySavedData hist = SnowHistorySavedData.get();
        hist.currentStormId = 0;
        hist.snowHistory.clear();
        hist.setDirty();
    }

    @Override
    public void onRainChanged(ServerLevel level, boolean isRaining) {
        if (!isOverworld(level)) return;

        SnowData data = getOrCreateData(level);

        if (isRaining && EnvironmentHelper.isSnowySeason()) {
            if (data.activeStorms.isEmpty()) {
                int stormId = data.stormCount + 1;
                data.activeStorms.add(stormId);
                // Create and register an active storm record at start so piling can be random immediately
                SnowHistorySavedData hist = SnowHistorySavedData.get();
                hist.currentStormId = stormId;
                // Generate and store the record now; computeGlobal* exclude currentStormId
                if (!hist.snowHistory.containsKey(stormId)) {
                    SnowRecord rec = SnowGenerator.generateStormRecord(level.random);
                    hist.snowHistory.put(stormId, rec);
                }
                hist.setDirty();
            }
        } else {
            if (!data.activeStorms.isEmpty()) {
                int endedStormId = data.activeStorms.iterator().next();
                data.activeStorms.remove(endedStormId);

                SnowHistorySavedData hist = SnowHistorySavedData.get();
                // Ensure an entry exists for the ended storm; it may already have been created at start
                if (!hist.snowHistory.containsKey(endedStormId)) {
                    SnowRecord rec = SnowGenerator.generateStormRecord(level.random);
                    hist.snowHistory.put(endedStormId, rec);
                }
                // Persist the finished storm count
                data.stormCount++;
                // Clear active storm marker
                hist.currentStormId = 0;

                hist.setDirty();
            }
        }
        persist(level, data);
    }

    @Override
    public int getSnowStormsThisWinter(ServerLevel level) {
        if (!isOverworld(level)) return 0;
        SnowData data = getOrCreateData(level);
        return data.stormCount - data.activeStorms.size();
    }

    @Override
    public void clear(ServerLevel level) {
        if (!isOverworld(level)) return;
        if (overworldData != null) {
            persist(level, overworldData);
            overworldData = null;
        }
    }

    @Override
    public boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos) {
        return isOverworld(level) && SeasonHooks.coldEnoughToSnowSeasonal(level, pos);
    }


    @Override
    public void onRainCloudSpawned(ServerLevel level, int hashCode) {
        // no-op
    }

    /** Called when a rainy cloud despawns */
    @Override
    public void onRainCloudDespawned(ServerLevel level, int hashCode) {
        // no-op
    }
}

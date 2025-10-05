package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.storage.SnowRecord;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SnowGenerator;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.season.SeasonHooks;

import java.util.HashSet;
import java.util.Set;

/**
 * Snow environment handler for Project Atmosphere.
 * Multiple storm sessions can run concurrently (one per rainy cloud).
 */
public class NeoForgeSnowEnvironmentHandler extends DefaultSnowEnvironmentHandler {

    private final Set<Integer> activeStorms = new HashSet<>();

    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        if (!SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded) {
            float temperature = SeasonHooks.getBiomeTemperature(level, level.getBiome(playerPos), playerPos);
            return SeasonHooks.coldEnoughToSnowSeasonal(level, playerPos)
                    ? CommonSnowBlockFeature.calculateBlocksToReplace(temperature)
                    : 0;
        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos),
                    level.getDayTime()
            );
            return temperature >= 0.5F
                    ? CommonSnowBlockFeature.calculateBlocksToReplace1(temperature)
                    : -level.random.nextInt(2, 6);
        }
    }

    @Override
    public boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos) {
        if (!SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded) {
            return SeasonHooks.coldEnoughToSnowSeasonal(level, pos);
        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(pos).unwrapKey().get().location(), pos),
                    level.getDayTime()
            );
            return temperature < 0.5F;
        }
    }



    public void onRainCloudSpawned(ServerLevel level, int hashCode) {
        if (!EnvironmentHelper.isSnowySeason()) return;
        SnowData data = getOrCreateData(level);
        if (data == null) return;

        data.activeStorms.add(hashCode);
    }

    /** Called when a rainy cloud despawns */
    public void onRainCloudDespawned(ServerLevel level, int hashCode) {
        SnowData data = getOrCreateData(level);
        if (data == null || !data.activeStorms.contains(hashCode)) return;

        data.activeStorms.remove(hashCode);

        SnowHistorySavedData hist = SnowHistorySavedData.get(level);
        SnowRecord rec = SnowGenerator.generateStormRecord(level.random);

        data.stormCount++;
        hist.currentStormId = data.stormCount;
        hist.snowHistory.put(hist.currentStormId, rec);
        SnowHistorySavedData.get(level).snowHistory.put(hist.currentStormId, rec);

        hist.setDirty();
        persist(level, data);
    }



}

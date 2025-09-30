package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.Gabou.projectatmosphere.api.AtmoApi;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonHooks;

public class NeoForgeSnowEnvironmentHandler extends DefaultSnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        if (!SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded) {
            float temperature = SeasonHooks.getBiomeTemperature(level,level.getBiome(playerPos),playerPos);
            if (SeasonHooks.coldEnoughToSnowSeasonal(level, playerPos)) {
                return CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
            }

        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos),
                    level.getDayTime()
            );

            if (temperature >= 0.5F) {
                return CommonSnowBlockFeature.calculateBlocksToReplace1(temperature);
            }
        }
        return -level.random.nextInt(2,6);
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
    @Override
    public void onRainChanged(ServerLevel level, net.minecraft.world.level.ChunkPos chunkPos, boolean isRaining, ISnowTrackedChunk trackedChunk) {
        if (!SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded) {
            // Defer to default behavior when PA is not present
            super.onRainChanged(level, chunkPos, isRaining, trackedChunk);
            return;
        }

        // With Project Atmosphere, use global snowing indicator for storm lifecycle,
        // and local precipitation for per-chunk pending flags.
        boolean snowySeason = EnvironmentHelper.isSnowySeason();
        boolean globalSnowing = AtmoApi.getInstance().isRainningAt(level);

        SnowData data = data(level);
        long key = chunkPos.toLong();

        if (globalSnowing) {
            if (snowySeason && !data.stormActive) {
                data.stormActive = true;
                data.stormCount++;
            } else if (!snowySeason) {
                data.stormActive = false;
            }

            if (snowySeason && isRaining) {
                if (data.pendingChunks.add(key)) {
                    data.observedChunks.add(key);
                }
                trackedChunk.sereneseasonsplus$setShouldApplyInitialSnow(true);
                trackedChunk.sereneseasonsplus$willReceiveSnow(true);
                ChunkQueue.enqueueScheduled(chunkPos);
            }
        } else {
            boolean wasActive = data.stormActive;
            data.stormActive = false;
            if (wasActive && data.stormCount > 1 && data.lastBlanketStormCount < data.stormCount) {
                blanketApplyLoadedChunks(level);
                data.lastBlanketStormCount = data.stormCount;
            }
        }

        persist(level, data);
    }



}
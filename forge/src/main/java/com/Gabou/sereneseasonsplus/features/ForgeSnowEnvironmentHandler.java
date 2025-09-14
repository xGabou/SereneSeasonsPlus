package com.Gabou.sereneseasonsplus.features;
import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;

import static com.Gabou.sereneseasonsplus.features.CommonSnowBlockReplacer.*;

public class ForgeSnowEnvironmentHandler implements SnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        if (!SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
            float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

            if (temperature >= 0.15F) {
                return SnowBlockReplacer.calculateBlocksToReplace(temperature);
            }
        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos),
                    level.getDayTime()
            );

            if (temperature >= 0.5F) {
                return SnowBlockReplacer.calculateBlocksToReplace1(temperature);
            }
        }
        return 0;
    }

    @Override
    public void processChunks(Level level, BlockPos worldPos, Season.SubSeason currentSubSeason, ChunkPos chunkPos) {
        float temperature;
        if (!SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            temperature = SnowUtils.getCachedBiomeTemperature(level, worldPos, currentSubSeason);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: {} temp={} (SS scale)", chunkPos, temperature);
            if (temperature >= 0.5F) {
                chunksToClear.add(chunkPos);
                if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: queued clear {} (queue={})", chunkPos, chunksToClear.size());
            } else if (temperature >= 0.15F) {
                long t0 = System.nanoTime();
                accelerateMelt(level, chunkPos);
                meltingChunks.add(chunkPos);
                if (LOGGER.isDebugEnabled()) {
                    long dt = (System.nanoTime() - t0) / 1_000_000L;
                    LOGGER.debug("onChunkLoad: accelerated melt {} in {} ms; queued gradual melt (size={})", chunkPos, dt, meltingChunks.size());
                }
            }
        } else {
            temperature = ForecastOrchestrator.getCurrentTemperature(new BiomeInstanceKey(level.getBiome(worldPos).unwrapKey().get().location(), worldPos), level.getDayTime());
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: {} temp={} (PA scale)", chunkPos, temperature);
            if (temperature >= 10.0F) {
                chunksToClear.add(chunkPos);
                if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: queued clear {} (queue={})", chunkPos, chunksToClear.size());
            } else if (temperature >= 0.5F) {
                long t0 = System.nanoTime();
                accelerateMelt(level, chunkPos);
                meltingChunks.add(chunkPos);
                if (LOGGER.isDebugEnabled()) {
                    long dt = (System.nanoTime() - t0) / 1_000_000L;
                    LOGGER.debug("onChunkLoad: accelerated melt {} in {} ms; queued gradual melt (size={})", chunkPos, dt, meltingChunks.size());
                }
            }
        }
    }
}

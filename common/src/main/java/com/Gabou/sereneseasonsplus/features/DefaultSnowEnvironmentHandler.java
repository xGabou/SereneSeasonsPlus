package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import static com.Gabou.sereneseasonsplus.features.CommonSnowBlockReplacer.*;

public class DefaultSnowEnvironmentHandler implements SnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

        if (temperature >= 0.15F) {
            return CommonSnowBlockReplacer.calculateBlocksToReplace(temperature);
        }
        return 0;
    }

    @Override
    public void processChunks(Level level, BlockPos worldPos, Season.SubSeason currentSubSeason, ChunkPos chunkPos) {
        float temperature = SnowUtils.getCachedBiomeTemperature(level, worldPos, currentSubSeason);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: {} temp={} (SS scale)", chunkPos, temperature);
        if (temperature >= 0.5F) {
            chunksToClear.add(chunkPos);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("onChunkLoad: queued clear {} (queue={})", chunkPos, chunksToClear.size());
        } else if (temperature >= 0.15F) {
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

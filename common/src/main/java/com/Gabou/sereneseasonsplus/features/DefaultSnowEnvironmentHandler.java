package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.storage.SnowRecord;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.util.SnowGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import sereneseasons.api.season.Season;
import sereneseasons.season.SeasonHooks;

public class DefaultSnowEnvironmentHandler implements SnowEnvironmentHandler {

    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        float temperature = SeasonHooks.getBiomeTemperature(level, level.getBiome(playerPos), playerPos);
        if (SeasonHooks.coldEnoughToSnowSeasonal(level, playerPos)) {
            return CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
        }
        return 0;
    }

    @Override
    public int getSnowStormsThisWinter(ServerLevel level) {
        var history = SnowHistorySavedData.get(level);
        return history.currentStormId;
    }

    @Override
    public boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos) {
        return SeasonHooks.coldEnoughToSnowSeasonal(level, pos);
    }

    protected void blanketApplyLoadedChunks(ServerLevel level) {
        if (!EnvironmentHelper.isSnowySeason()) return;
        Season.SubSeason current = EnvironmentHelper.getCurrentSeason();
        if (current == null) return;

        var chunkSource = level.getChunkSource();
        for (ServerPlayer player : level.players()) {
            int view = level.getServer() != null ? level.getServer().getPlayerList().getViewDistance() : 10;
            BlockPos center = player.blockPosition();
            int pcx = center.getX() >> 4;
            int pcz = center.getZ() >> 4;
            for (int dx = -view; dx <= view; dx++) {
                for (int dz = -view; dz <= view; dz++) {
                        CommonSnowBlockFeature.enqueueChunkForSnowApply(chunkSource.getChunk(pcx + dx, pcz + dz, false).getPos(), current);
                }
            }
        }
    }
}

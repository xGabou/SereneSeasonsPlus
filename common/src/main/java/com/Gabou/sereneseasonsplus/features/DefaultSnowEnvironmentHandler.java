package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.storage.SnowSavedData;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultSnowEnvironmentHandler implements SnowEnvironmentHandler {
    private static final class SnowData {
        int winterId = -1;
        int stormCount = 0;
        boolean stormActive = false;
        final Set<Long> pendingChunks = new HashSet<>();
        final Set<Long> observedChunks = new HashSet<>();
    }

    private final Map<ResourceKey<Level>, SnowData> perLevelData = new HashMap<>();

    private SnowData data(ServerLevel level) {
        return perLevelData.computeIfAbsent(level.dimension(), k -> {
            SnowData d = new SnowData();
            // Restore from persisted storage if present
            SnowSavedData store = SnowSavedData.get(level);
            d.winterId = store.winterId;
            d.stormCount = store.stormCount;
            d.stormActive = store.stormActive;
            d.pendingChunks.addAll(store.pendingChunks);
            d.observedChunks.addAll(store.observedChunks);
            return d;
        });
    }

    private void persist(ServerLevel level, SnowData d) {
        SnowSavedData store = SnowSavedData.get(level);
        store.winterId = d.winterId;
        store.stormCount = d.stormCount;
        store.stormActive = d.stormActive;
        store.pendingChunks.clear();
        store.pendingChunks.addAll(d.pendingChunks);
        store.observedChunks.clear();
        store.observedChunks.addAll(d.observedChunks);
        store.setDirty();
    }

    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

        if (temperature >= 0.15F) {
            return CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
        }
        return 0;
    }

    @Override
    public void resetWinterState(ServerLevel level, int winterId) {
        SnowData data = data(level);
        if (data.winterId == winterId) {
            return;
        }
        data.winterId = winterId;
        data.stormCount = 0;
        data.stormActive = false;
        data.pendingChunks.clear();
        data.observedChunks.clear();
        persist(level, data);
    }

    @Override
    public void onRainChanged(ServerLevel level, ChunkPos chunkPos, boolean isRaining) {
        SnowData data = data(level);
        long key = chunkPos.toLong();

        if (isRaining) {
            boolean snowySeason = EnvironmentHelper.isSnowySeason();
            if (snowySeason && !data.stormActive) {
                data.stormActive = true;
                data.stormCount++;
            } else if (!snowySeason) {
                data.stormActive = false;
            }
            if (snowySeason && data.pendingChunks.add(key)) {
                data.observedChunks.add(key);
            }

            if (snowySeason) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                if (chunk instanceof ISnowTrackedChunk tracked) {
                    tracked.sereneseasonsplus$setShouldApplyInitialSnow(true);
                    tracked.sereneseasonsplus$willReceiveSnow(true);
                }
                ChunkQueue.enqueueScheduled(chunkPos);
            }
        } else if (!EnvironmentHelper.isRainning(level, chunkPos.getMiddleBlockPosition(65))) {
            data.stormActive = false;
        }
        // Persist any changes in storm state / pending observations
        persist(level, data);
    }

    @Override
    public boolean shouldApplySnow(ServerLevel level, ChunkPos chunkPos) {
        return data(level).pendingChunks.contains(chunkPos.toLong());
    }

    @Override
    public void onSnowApplied(ServerLevel level, ChunkPos chunkPos, boolean success) {
        SnowData data = data(level);
        long key = chunkPos.toLong();
        if (success) {
            data.pendingChunks.remove(key);
        }
        data.observedChunks.add(key);
        persist(level, data);
    }

    @Override
    public int getSnowStormsThisWinter(ServerLevel level) {
        return data(level).stormCount;
    }

    @Override
    public boolean hasChunkSeenSnow(ServerLevel level, ChunkPos chunkPos) {
        return data(level).observedChunks.contains(chunkPos.toLong());
    }

    @Override
    public void clear(ServerLevel level) {
        SnowData d = perLevelData.remove(level.dimension());
        if (d != null) {
            persist(level, d);
        }
    }
}

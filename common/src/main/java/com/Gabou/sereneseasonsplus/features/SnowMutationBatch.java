package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SnowMutationBatch {
    private final Map<BlockPos, SnowWorldMutation> pendingChanges = new LinkedHashMap<>();
    private final Map<Long, Integer> pendingChunkChangeCounts = new HashMap<>();
    private final Set<ChunkPos> chunksToDirty = new HashSet<>();
    private final Map<ChunkPos, Map<BlockPos, Integer>> pendingColumnMapUpdates = new HashMap<>();
    private final Map<ChunkPos, Set<BlockPos>> pendingIceAdds = new HashMap<>();

    public void clear() {
        pendingChanges.clear();
        pendingChunkChangeCounts.clear();
        chunksToDirty.clear();
        pendingColumnMapUpdates.clear();
        pendingIceAdds.clear();
    }

    public int pendingChangeCount() {
        return pendingChanges.size();
    }

    public boolean hasPendingChanges() {
        return !pendingChanges.isEmpty();
    }

    public void markChunkDirty(ChunkPos chunkPos) {
        chunksToDirty.add(chunkPos);
    }

    public boolean hasPendingWorkForChunk(ChunkPos chunkPos) {
        if (pendingColumnMapUpdates.containsKey(chunkPos) || pendingIceAdds.containsKey(chunkPos)) {
            return true;
        }
        return pendingChunkChangeCounts.containsKey(ChunkPos.asLong(chunkPos.x, chunkPos.z));
    }

    public boolean queueClearIfNeeded(ServerLevel level, BlockPos pos, boolean toWater) {
        BlockState current = level.getBlockState(pos);
        BlockState wanted;
        if (current.is(Blocks.ICE) || current.is(Blocks.FROSTED_ICE)) {
            wanted = Blocks.WATER.defaultBlockState();
        } else {
            wanted = toWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (current.is(wanted.getBlock())) {
            return false;
        }
        queueChange(pos, wanted, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        return true;
    }

    public void queueChange(BlockPos pos, BlockState state, int flags) {
        queueMutation(SnowWorldMutation.setBlock(pos, state, flags));
    }

    public void queueMutation(SnowWorldMutation mutation) {
        BlockPos immutable = mutation.key().immutable();
        SnowWorldMutation previous = pendingChanges.put(immutable, mutation);
        if (previous == null) {
            long chunkKey = ChunkPos.asLong(immutable.getX() >> 4, immutable.getZ() >> 4);
            pendingChunkChangeCounts.merge(chunkKey, 1, Integer::sum);
        }
    }

    public void accumulateColumnUpdate(ServerLevel level, BlockPos pos, BlockState state, SnowBlockCompatibility compatibility) {
        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        if (compatibility.isManagedIce(state)) {
            pendingIceAdds.computeIfAbsent(chunkPos, k -> new HashSet<>()).add(pos.immutable());
            chunksToDirty.add(chunkPos);
            return;
        }

        int value = compatibility.getManagedLayers(level, pos, state);
        pendingColumnMapUpdates
                .computeIfAbsent(chunkPos, k -> new HashMap<>())
                .put(pos.immutable(), value);
        chunksToDirty.add(chunkPos);
    }

    public int processQueuedChanges(ServerLevel level, int limit, SnowBlockCompatibility compatibility) {
        return processQueuedChanges(level, 0, limit, Long.MAX_VALUE, compatibility);
    }

    public int processQueuedChanges(ServerLevel level,
                                    int minToProcess,
                                    int maxToProcess,
                                    long deadlineNanos,
                                    SnowBlockCompatibility compatibility) {
        if (pendingChanges.isEmpty()) {
            return 0;
        }

        int applied = 0;
        Iterator<Map.Entry<BlockPos, SnowWorldMutation>> iterator = pendingChanges.entrySet().iterator();
        while (iterator.hasNext() && applied < maxToProcess) {
            if (applied >= minToProcess && System.nanoTime() >= deadlineNanos) {
                break;
            }
            Map.Entry<BlockPos, SnowWorldMutation> entry = iterator.next();
            SnowWorldMutation mutation = entry.getValue();
            if (mutation.apply(level)) {
                BlockPos trackingPos = mutation.trackingPos();
                accumulateColumnUpdate(level, trackingPos, level.getBlockState(trackingPos), compatibility);
            }
            long chunkKey = ChunkPos.asLong(mutation.key().getX() >> 4, mutation.key().getZ() >> 4);
            pendingChunkChangeCounts.computeIfPresent(chunkKey, (key, count) -> count > 1 ? count - 1 : null);
            iterator.remove();
            applied++;
        }

        return applied;
    }

    public void finalizeChunkBatch(ServerLevel level) {
        if (!pendingColumnMapUpdates.isEmpty()) {
            for (Map.Entry<ChunkPos, Map<BlockPos, Integer>> entry : pendingColumnMapUpdates.entrySet()) {
                ChunkPos chunkPos = entry.getKey();
                if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                if (!(chunk instanceof ISnowTrackedChunk tracked)) {
                    continue;
                }

                Map<BlockPos, Integer> columns = tracked.sereneseasonsplus$getSnowColumns();
                for (Map.Entry<BlockPos, Integer> update : entry.getValue().entrySet()) {
                    int value = update.getValue();
                    if (value <= 0) {
                        columns.remove(update.getKey());
                    } else {
                        columns.put(update.getKey().immutable(), Mth.clamp(value, 1, 8));
                    }
                }
            }
            pendingColumnMapUpdates.clear();
        }

        if (!pendingIceAdds.isEmpty()) {
            for (Map.Entry<ChunkPos, Set<BlockPos>> entry : pendingIceAdds.entrySet()) {
                ChunkPos chunkPos = entry.getKey();
                if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                if (!(chunk instanceof ISnowTrackedChunk tracked)) {
                    continue;
                }

                Set<BlockPos> iceColumns = tracked.sereneseasonsplus$getIceColumns();
                for (BlockPos pos : entry.getValue()) {
                    if (level.getBlockState(pos).is(Blocks.ICE)) {
                        iceColumns.add(pos.immutable());
                    }
                }
            }
            pendingIceAdds.clear();
        }

        for (ChunkPos chunkPos : chunksToDirty) {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            if (chunk != null) {
                chunk.setUnsaved(true);
            }
        }

        chunksToDirty.clear();
    }
}

package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.tags.SSPTags;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public final class SnowChunkMeltService {
    private final SnowStateService stateService;

    public SnowChunkMeltService(SnowStateService stateService) {
        this.stateService = stateService;
    }

    public boolean meltSnowInChunk(ServerLevel level, ChunkPos chunkPos, boolean fullClear) {
        LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
        if (!(chunk instanceof ISnowTrackedChunk tracked)) {
            return false;
        }

        if (fullClear) {
            return clearTrackedSnowImmediately(
                    level,
                    chunk,
                    tracked,
                    CommonSnowBlockFeature.LIVE_MELT_MUTATION_FLAGS
            );
        }

        boolean changed = false;
        Map<BlockPos, Integer> columns = stateService.getSnowColumns(tracked);
        if (columns == null) {
            columns = Collections.emptyMap();
        }

        if (!columns.isEmpty()) {
            Map<Long, BlockPos> topByColumn = stateService.getTopTrackedSnowByColumn(tracked);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (BlockPos top : topByColumn.values()) {
                BlockState state = level.getBlockState(top);
                if (CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(state)) {
                    int layers = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getManagedLayers(level, top, state);
                    if (layers <= 1) {
                        changed |= CommonSnowBlockFeature.queueClearIfNeeded(level, top, false);
                        stateService.removeTrackedColumn(tracked, top);
                    } else {
                        changed |= CommonSnowBlockFeature.queueSnowLayersIfNeeded(level, top, layers - 1, false);
                        stateService.setTrackedLayers(tracked, top.immutable(), layers - 1);
                    }
                } else {
                    int minY = level.getMinY();
                    cursor.set(top.getX(), top.getY(), top.getZ());
                    while (cursor.getY() >= minY) {
                        BlockState scanned = level.getBlockState(cursor);
                        if (CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(scanned)) {
                            int layers = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getManagedLayers(level, cursor.immutable(), scanned);
                            if (layers <= 1) {
                                changed |= CommonSnowBlockFeature.queueClearIfNeeded(level, cursor.immutable(), false);
                                stateService.removeTrackedColumn(tracked, cursor.immutable());
                            } else {
                                changed |= CommonSnowBlockFeature.queueSnowLayersIfNeeded(level, cursor.immutable(), layers - 1, false);
                                stateService.setTrackedLayers(tracked, cursor.immutable(), layers - 1);
                            }
                            break;
                        }
                        cursor.move(0, -1, 0);
                    }
                }
            }
        }

        changed |= clearCoveredMeltablesNearSurface(level, chunk);
        changed |= meltTrackedIce(level, tracked);
        return changed;
    }

    /**
     * Clears only positions owned by SSP's persisted chunk index. This avoids a
     * 16x16 surface scan and, during the chunk-load callback, makes the first
     * chunk packet already contain the melted state.
     */
    public boolean meltSnowInChunkImmediately(ServerLevel level, LevelChunk chunk) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) {
            return false;
        }

        return clearTrackedSnowImmediately(
                level,
                chunk,
                tracked,
                CommonSnowBlockFeature.CHUNK_LOAD_MUTATION_FLAGS
        );
    }

    private boolean clearTrackedSnowImmediately(ServerLevel level,
                                                LevelChunk chunk,
                                                ISnowTrackedChunk tracked,
                                                int mutationFlags) {
        boolean changed = false;
        boolean metadataChanged = false;
        Map<BlockPos, Integer> columns = stateService.getSnowColumns(tracked);
        if (columns != null && !columns.isEmpty()) {
            for (BlockPos pos : new ArrayList<>(columns.keySet())) {
                changed |= clearManagedSnowCompletely(level, pos, mutationFlags);
                if (!CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(level.getBlockState(pos))) {
                    stateService.removeTrackedColumn(tracked, pos);
                    metadataChanged = true;
                }
            }
        }

        for (BlockPos pos : new java.util.HashSet<>(tracked.sereneseasonsplus$getIceColumns())) {
            BlockState state = level.getBlockState(pos);
            if (CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedIce(state)) {
                SnowWorldMutation mutation = CommonSnowBlockFeature.SNOW_COMPATIBILITY.createClearMutation(
                        level,
                        pos,
                        state,
                        true,
                        mutationFlags
                );
                changed |= mutation != null && mutation.apply(level);
            }
            if (!CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedIce(level.getBlockState(pos))) {
                tracked.sereneseasonsplus$getIceColumns().remove(pos);
                metadataChanged = true;
            }
        }

        if (tracked.sereneseasonsplus$getAppliedStormCount() != 0) {
            tracked.sereneseasonsplus$setAppliedStormCount(0);
            metadataChanged = true;
        }
        if (changed || metadataChanged) {
            chunk.markUnsaved();
        }
        return changed;
    }

    private boolean clearManagedSnowCompletely(ServerLevel level, BlockPos pos, int mutationFlags) {
        boolean changed = false;
        for (int layer = 0; layer < 8; layer++) {
            BlockState state = level.getBlockState(pos);
            if (!CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(state)) {
                break;
            }
            SnowWorldMutation mutation = CommonSnowBlockFeature.SNOW_COMPATIBILITY.createClearMutation(
                    level,
                    pos,
                    state,
                    false,
                    mutationFlags
            );
            if (mutation == null || !mutation.apply(level)) {
                break;
            }
            changed = true;
        }
        if (changed) {
            CommonSnowBlockFeature.syncSnowyGroundState(level, pos, mutationFlags);
        }
        return changed;
    }

    public boolean clearCoveredMeltablesNearSurface(ServerLevel level, LevelChunk chunk) {
        boolean changed = false;
        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int dy = 0; dy <= 6; dy++) {
                    pos.set(x, groundY + dy, z);
                    if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.is(SSPTags.Blocks.MELTABLE) && !CommonSnowBlockFeature.isExposedToSky(level, pos)) {
                        changed |= CommonSnowBlockFeature.queueClearIfNeeded(level, pos.immutable(), false);
                        break;
                    }
                }
            }
        }
        return changed;
    }

    public boolean meltTrackedIce(ServerLevel level, ISnowTrackedChunk tracked) {
        boolean changed = false;
        java.util.Set<BlockPos> copy = new java.util.HashSet<>(tracked.sereneseasonsplus$getIceColumns());
        for (BlockPos pos : copy) {
            BlockState state = level.getBlockState(pos);
            if (CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedIce(state)) {
                CommonSnowBlockFeature.queueChangeIfStateMatches(
                        pos,
                        state,
                        Blocks.WATER.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
                );
                tracked.sereneseasonsplus$getIceColumns().remove(pos);
                changed = true;
            } else {
                tracked.sereneseasonsplus$getIceColumns().remove(pos);
            }
        }
        return changed;
    }
}

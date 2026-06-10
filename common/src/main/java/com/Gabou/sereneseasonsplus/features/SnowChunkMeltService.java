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
import java.util.HashMap;
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

        boolean changed = false;
        Map<BlockPos, Integer> columns = stateService.getSnowColumns(tracked);
        if (columns == null) {
            columns = Collections.emptyMap();
        }

        if (fullClear && !columns.isEmpty()) {
            for (BlockPos pos : new ArrayList<>(columns.keySet())) {
                changed |= CommonSnowBlockFeature.queueClearIfNeeded(level, pos, false);
                stateService.removeTrackedColumn(tracked, pos);
            }
        }

        if (!columns.isEmpty() && !fullClear) {
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
                    int minY = level.getMinBuildHeight();
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
                    if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
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
                CommonSnowBlockFeature.queueChange(
                        pos,
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

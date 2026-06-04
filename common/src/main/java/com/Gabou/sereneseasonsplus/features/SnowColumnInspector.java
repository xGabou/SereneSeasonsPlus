package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

public final class SnowColumnInspector {
    private SnowColumnInspector() {
    }

    public record ColumnSnapshot(BlockPos anchorPos, @Nullable BlockPos topManagedPos, int totalLayers) {
        public boolean hasManagedSnow() {
            return topManagedPos != null && totalLayers > 0;
        }
    }

    @Nullable
    public static BlockPos findPlacementTop(ServerLevel level, int x, int z, SnowBlockCompatibility compatibility) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos base = new BlockPos(x, y, z);
        var baseState = level.getBlockState(base);

        if (compatibility.canPlaceManagedSnow(level, base, baseState)) {
            return base;
        }

        BlockPos up = base.above();
        if (compatibility.canPlaceManagedSnow(level, up, level.getBlockState(up))) {
            return up;
        }

        if (compatibility.isManagedSnow(baseState) && CommonSnowBlockFeature.canReceiveSnowAt(level, base)) {
            return base;
        }

        return null;
    }

    public static int countAvailableColumns(ServerLevel level, LevelChunk chunk, SnowBlockCompatibility compatibility) {
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int count = 0;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                if (findPlacementTop(level, baseX + dx, baseZ + dz, compatibility) != null) {
                    count++;
                }
            }
        }

        return count;
    }

    @Nullable
    public static ColumnSnapshot inspectColumn(ServerLevel level, int x, int z, SnowBlockCompatibility compatibility) {
        BlockPos anchor = findPlacementTop(level, x, z, compatibility);
        if (anchor == null) {
            return null;
        }

        BlockPos.MutableBlockPos down = new BlockPos.MutableBlockPos(anchor.getX(), anchor.getY(), anchor.getZ());
        if (!compatibility.isManagedSnow(level.getBlockState(down))) {
            down.move(0, -1, 0);
        }

        int totalLayers = 0;
        BlockPos topManagedPos = null;
        int minY = level.getMinY();
        while (down.getY() >= minY) {
            var state = level.getBlockState(down);
            if (!compatibility.isManagedSnow(state)) {
                break;
            }
            if (topManagedPos == null) {
                topManagedPos = down.immutable();
            }
            totalLayers += compatibility.getManagedLayers(level, down.immutable(), state);
            down.move(0, -1, 0);
        }

        return new ColumnSnapshot(anchor, topManagedPos, totalLayers);
    }

    public static int computeManagedColumnTotal(ServerLevel level, BlockPos pos, SnowBlockCompatibility compatibility) {
        int total = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        if (!compatibility.isManagedSnow(level.getBlockState(cursor))) {
            cursor.move(0, -1, 0);
        }

        while (cursor.getY() >= minY) {
            var state = level.getBlockState(cursor);
            if (!compatibility.isManagedSnow(state)) {
                break;
            }
            total += compatibility.getManagedLayers(level, cursor.immutable(), state);
            cursor.move(0, -1, 0);
        }

        cursor.set(pos.getX(), pos.getY() + 1, pos.getZ());
        while (cursor.getY() < maxY) {
            var state = level.getBlockState(cursor);
            if (!compatibility.isManagedSnow(state)) {
                break;
            }
            total += compatibility.getManagedLayers(level, cursor.immutable(), state);
            cursor.move(0, 1, 0);
        }

        return total;
    }
}

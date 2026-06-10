package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class ActiveSnowUpdateService {
    public void run(ServerLevel level, Map<ServerPlayer, BlockPos> playerPositions) {
        if (EnvironmentHelper.isSnowRealMagicLoaded()) {
            return;
        }
        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            int simulationDistance = CommonSnowBlockFeature.getSimulationDistance(player);
            int radius = Mth.clamp(simulationDistance * 16, 16, 64);
            int playerIntent = CommonSnowBlockFeature.HANDLER.getBlocksToReplace(level, playerPos);

            if (playerIntent < 0) {
                runLocalAccumulation(level, playerPos, radius);
            } else if (playerIntent > 0) {
                runLocalMelting(level, playerPos, radius, playerIntent);
            }
        }
    }

    private void runLocalAccumulation(ServerLevel level, BlockPos playerPos, int radius) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < CommonSnowBlockFeature.MAX_ATTEMPTS; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int x = playerPos.getX() + dx;
            int z = playerPos.getZ() + dz;

            BlockPos surface = CommonSnowBlockFeature.findPlacementTop(level, x, z);
            if (surface == null) continue;
            if (!EnvironmentHelper.isRainning(level, surface)) continue;
            if (CommonSnowBlockFeature.HANDLER.getBlocksToReplace(level, surface) >= 0) continue;

            BlockState state = level.getBlockState(surface);
            BlockPos targetPos = surface;
            int targetLayers = 1;

            if (state.is(Blocks.SNOW)) {
                int currentLayers = state.getValue(SnowLayerBlock.LAYERS);
                if (currentLayers < 8) {
                    targetLayers = currentLayers + 1;
                } else {
                    BlockPos above = surface.above();
                    if (!level.isEmptyBlock(above)) continue;
                    targetPos = above;
                }
            } else if (state.is(Blocks.SNOW_BLOCK)) {
                BlockPos above = surface.above();
                if (!level.isEmptyBlock(above)) continue;
                targetPos = above;
            }

            if (CommonSnowBlockFeature.placeOrQueueLayers(level, targetPos, targetLayers, true, false)) {
                CommonSnowBlockFeature.accumulateColumnUpdate(targetPos, level.getBlockState(targetPos));
            }
        }
    }

    private void runLocalMelting(ServerLevel level, BlockPos playerPos, int radius, int attempts) {
        for (int i = 0; i < attempts; ++i) {
            BlockPos targetPos = sampleLocalMeltTarget(level, playerPos, radius, Math.max(12, Math.min(48, attempts * 4)));
            if (targetPos == null) continue;
            if (CommonSnowBlockFeature.HANDLER.getBlocksToReplace(level, targetPos) <= 0) continue;

            BlockState state = level.getBlockState(targetPos);
            if (!CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(state)) {
                continue;
            }

            int layers = CommonSnowBlockFeature.SNOW_COMPATIBILITY.getManagedLayers(level, targetPos, state);
            if (layers <= 1) {
                CommonSnowBlockFeature.queueClearIfNeeded(level, targetPos, false);
            } else {
                CommonSnowBlockFeature.queueSnowLayersIfNeeded(level, targetPos, layers - 1, false);
            }
        }
    }

    private BlockPos sampleLocalMeltTarget(ServerLevel level, BlockPos playerPos, int radius, int sampleBudget) {
        RandomSource random = level.random;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int sample = 0; sample < sampleBudget; sample++) {
            int x = playerPos.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = playerPos.getZ() + random.nextInt(radius * 2 + 1) - radius;
            int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            for (int dy = 4; dy >= -4; dy--) {
                cursor.set(x, groundY + dy, z);
                if (cursor.getY() < level.getMinBuildHeight() || cursor.getY() >= level.getMaxBuildHeight()) {
                    continue;
                }

                BlockState state = level.getBlockState(cursor);
                if (!CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(state)) {
                    continue;
                }
                return cursor.immutable();
            }
        }

        return null;
    }
}

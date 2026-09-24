package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface SnowBlockCompatibility {
    boolean isManagedSnow(BlockState state);

    boolean isManagedIce(BlockState state);

    boolean isReplaceableForSnow(BlockState state);

    int getManagedLayers(BlockState state);

    default int getManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        return getManagedLayers(state);
    }

    default int getMaxManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        return 8;
    }

    BlockState createManagedSnow(int layers);

    @Nullable
    default SnowWorldMutation createLayerMutation(ServerLevel level,
                                                  BlockPos pos,
                                                  BlockState state,
                                                  int targetLayers,
                                                  boolean allowPlace) {
        return createLayerMutation(
                level,
                pos,
                state,
                targetLayers,
                allowPlace,
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS
        );
    }

    @Nullable
    default SnowWorldMutation createLayerMutation(ServerLevel level,
                                                  BlockPos pos,
                                                  BlockState state,
                                                  int targetLayers,
                                                  boolean allowPlace,
                                                  int flags) {
        if (isManagedSnow(state)) {
            if (allowPlace && !CommonSnowBlockFeature.canReceiveSnowAt(level, pos)) {
                return null;
            }
            BlockState newState = createManagedSnow(targetLayers);
            return SnowWorldMutation.setBlockIfStateMatches(
                    pos, state, newState, flags);
        }
        if (allowPlace && (state.isAir() || isReplaceableForSnow(state))) {
            if (!canPlaceManagedSnow(level, pos, state)) {
                return null;
            }
            BlockState snow = createManagedSnow(targetLayers);
            return SnowWorldMutation.setBlockIfStateMatches(
                    pos, state, snow, flags);
        }
        return null;
    }

    @Nullable
    default SnowWorldMutation createClearMutation(ServerLevel level, BlockPos pos, BlockState state, boolean toWater) {
        return createClearMutation(
                level,
                pos,
                state,
                toWater,
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                        | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS
        );
    }

    @Nullable
    default SnowWorldMutation createClearMutation(ServerLevel level,
                                                  BlockPos pos,
                                                  BlockState state,
                                                  boolean toWater,
                                                  int flags) {
        BlockState target;
        if (isManagedIce(state)) {
            target = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        } else if (isManagedSnow(state)) {
            target = toWater
                    ? net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        } else {
            return null;
        }
        if (state.is(target.getBlock())) {
            return null;
        }
        return SnowWorldMutation.setBlockIfStateMatches(
                pos,
                state,
                target,
                flags
        );
    }

    default boolean canPlaceManagedSnow(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState snow = createManagedSnow(1);
        return (state.isAir() || isReplaceableForSnow(state))
                && snow.canSurvive(level, pos)
                && CommonSnowBlockFeature.canReceiveSnowAt(level, pos);
    }
}

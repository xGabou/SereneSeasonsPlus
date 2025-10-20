package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SnowUtils {
    /**
     * Decrements a layered block (e.g., snow) if possible; otherwise removes
     * the block entirely by setting air.
     *
     * @param level level to modify
     * @param pos   target position
     */
    public static void breakOrDecrementLayer(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> doBreakOrDecrementLayer(serverLevel, pos));
        } else {
            doBreakOrDecrementLayer(level, pos);
        }
    }

    private static void doBreakOrDecrementLayer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.LAYERS)) {
            int layers = state.getValue(BlockStateProperties.LAYERS);
            if (layers > 1) {
                BlockState newState = state.setValue(BlockStateProperties.LAYERS, layers - 1);
                if (!newState.equals(state)) {
                    level.setBlock(
                            pos,
                            newState,
                            Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
                    );
                }
                return;
            }
        }
        if (!level.getBlockState(pos).is(Blocks.AIR)) {
            level.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
            );
        }
    }


}

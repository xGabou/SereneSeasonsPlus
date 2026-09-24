package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface SnowWorldMutation {
    BlockPos key();

    boolean apply(ServerLevel level);

    default BlockPos trackingPos() {
        return key();
    }

    static SnowWorldMutation setBlockIfStateMatches(BlockPos pos, BlockState expectedState, BlockState state, int flags) {
        return new SetBlockMutation(pos.immutable(), expectedState, state, flags);
    }

    record SetBlockMutation(BlockPos key, BlockState expectedState, BlockState state, int flags) implements SnowWorldMutation {
        @Override
        public boolean apply(ServerLevel level) {
            if (expectedState != null && !level.getBlockState(key).equals(expectedState)) {
                return false;
            }
            return level.setBlock(key, state, flags);
        }
    }
}

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

    static SnowWorldMutation setBlock(BlockPos pos, BlockState state, int flags) {
        return new SetBlockMutation(pos.immutable(), state, flags);
    }

    static SnowWorldMutation setBlockIfUnchanged(BlockPos pos, BlockState expected, BlockState state, int flags) {
        return new GuardedSetBlockMutation(pos.immutable(), expected, state, flags);
    }

    record GuardedSetBlockMutation(BlockPos key, BlockState expected, BlockState state, int flags) implements SnowWorldMutation {
        @Override
        public boolean apply(ServerLevel level) {
            return level.getBlockState(key).equals(expected) && level.setBlock(key, state, flags);
        }
    }

    record SetBlockMutation(BlockPos key, BlockState state, int flags) implements SnowWorldMutation {
        @Override
        public boolean apply(ServerLevel level) {
            return level.setBlock(key, state, flags);
        }
    }
}

package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ServerPrecipitationService {
    private ServerPrecipitationService() {
    }

    public static boolean isDestroyedDuringCurrentStorm(LevelChunk chunk, BlockPos pos) {
        return false;
    }

    public static boolean canPlaceSnowWithoutReplacingImportant(ServerLevel level, BlockPos pos, BlockState newState) {
        if (!newState.is(Blocks.SNOW)) {
            return true;
        }

        BlockState current = level.getBlockState(pos);
        return current.isAir()
                || CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(current)
                || CommonSnowBlockFeature.SNOW_COMPATIBILITY.isReplaceableForSnow(current);
    }

    public static boolean setBlockAndTrackSnow(ServerLevel level, BlockPos pos, BlockState state) {
        boolean result = level.setBlockAndUpdate(pos, state);
        if (result) {
            CommonSnowBlockFeature.accumulateColumnUpdate(level, pos, state);
        }
        return result;
    }

    public static boolean shouldTreatAsSnow(BlockState state, Block block, boolean skipSnowCheck) {
        if (skipSnowCheck && block == Blocks.SNOW) {
            return false;
        }
        return state.is(block);
    }
}

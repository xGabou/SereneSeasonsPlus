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
        SnowHistorySavedData savedData = SnowHistorySavedData.get();
        int activeStormId = savedData != null ? savedData.currentStormId : 0;
        if (activeStormId <= 0 || !(chunk instanceof ISnowTrackedChunk tracked)) {
            return false;
        }

        if (tracked.sereneseasonsplus$getDestroyedStormId() != activeStormId) {
            tracked.sereneseasonsplus$getDestroyedColumns().clear();
            tracked.sereneseasonsplus$setDestroyedStormId(activeStormId);
        }

        long xz = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xFFFFFFFFL);
        return tracked.sereneseasonsplus$getDestroyedColumns().contains(xz);
    }

    public static boolean canPlaceSnowWithoutReplacingImportant(ServerLevel level, BlockPos pos, BlockState newState) {
        if (!newState.is(Blocks.SNOW)) {
            return true;
        }

        BlockState current = level.getBlockState(pos);
        return CommonSnowBlockFeature.SNOW_COMPATIBILITY.isManagedSnow(current)
                || current.isAir()
                || CommonSnowBlockFeature.SNOW_COMPATIBILITY.isReplaceableForSnow(current);
    }

    public static boolean setBlockAndTrackSnow(ServerLevel level, BlockPos pos, BlockState state) {
        boolean result = level.setBlockAndUpdate(pos, state);
        if (result) {
            CommonSnowBlockFeature.accumulateColumnUpdate(pos, state);
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

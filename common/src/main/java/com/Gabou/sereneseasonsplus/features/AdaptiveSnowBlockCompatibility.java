package com.Gabou.sereneseasonsplus.features;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class AdaptiveSnowBlockCompatibility implements SnowBlockCompatibility {
    private final VanillaSnowBlockCompatibility vanilla = new VanillaSnowBlockCompatibility();
    private final SnowRealMagicCompatibilityAdapter snowRealMagic = new SnowRealMagicCompatibilityAdapter();

    @Override
    public boolean isManagedSnow(BlockState state) {
        return vanilla.isManagedSnow(state) || snowRealMagic.isManagedSnow(state);
    }

    @Override
    public boolean isManagedIce(BlockState state) {
        return vanilla.isManagedIce(state);
    }

    @Override
    public boolean isReplaceableForSnow(BlockState state) {
        return vanilla.isReplaceableForSnow(state);
    }

    @Override
    public int getManagedLayers(BlockState state) {
        return vanilla.getManagedLayers(state);
    }

    @Override
    public int getManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        if (snowRealMagic.isManagedSnow(state)) {
            return snowRealMagic.getManagedLayers(level, pos, state);
        }
        return vanilla.getManagedLayers(level, pos, state);
    }

    @Override
    public int getMaxManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        if (snowRealMagic.isManagedSnow(state)) {
            return snowRealMagic.getMaxManagedLayers(level, pos, state);
        }
        return vanilla.getMaxManagedLayers(level, pos, state);
    }

    @Override
    public BlockState createManagedSnow(int layers) {
        return vanilla.createManagedSnow(layers);
    }

    @Override
    public boolean canPlaceManagedSnow(ServerLevel level, BlockPos pos, BlockState state) {
        if (snowRealMagic.canManagePlacementState(state)) {
            return snowRealMagic.canPlaceManagedSnow(level, pos, state);
        }
        return vanilla.canPlaceManagedSnow(level, pos, state);
    }

    @Override
    public @Nullable SnowWorldMutation createLayerMutation(ServerLevel level,
                                                           BlockPos pos,
                                                           BlockState state,
                                                           int targetLayers,
                                                           boolean allowPlace) {
        if (snowRealMagic.canManagePlacementState(state)) {
            SnowWorldMutation mutation = snowRealMagic.createLayerMutation(level, pos, state, targetLayers, allowPlace);
            if (mutation != null) {
                return mutation;
            }
        }
        return vanilla.createLayerMutation(level, pos, state, targetLayers, allowPlace);
    }

    @Override
    public @Nullable SnowWorldMutation createClearMutation(ServerLevel level,
                                                           BlockPos pos,
                                                           BlockState state,
                                                           boolean toWater) {
        if (snowRealMagic.isManagedSnow(state)) {
            SnowWorldMutation mutation = snowRealMagic.createClearMutation(pos, state, toWater);
            if (mutation != null) {
                return mutation;
            }
        }
        return vanilla.createClearMutation(level, pos, state, toWater);
    }
}

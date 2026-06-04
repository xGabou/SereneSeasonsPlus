package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.tags.SSPTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class VanillaSnowBlockCompatibility implements SnowBlockCompatibility {
    @Override
    public boolean isManagedSnow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    @Override
    public boolean isManagedIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE);
    }

    @Override
    public boolean isReplaceableForSnow(BlockState state) {
        return state.is(SSPTags.Blocks.SNOW_REPLACEABLE);
    }

    @Override
    public int getManagedLayers(BlockState state) {
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            return state.getValue(SnowLayerBlock.LAYERS);
        }
        if (state.is(Blocks.SNOW_BLOCK)) {
            return 8;
        }
        return 0;
    }

    @Override
    public int getManagedLayers(ServerLevel level, BlockPos pos, BlockState state) {
        return getManagedLayers(state);
    }

    @Override
    public BlockState createManagedSnow(int layers) {
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Mth.clamp(layers, 1, 8));
    }
}

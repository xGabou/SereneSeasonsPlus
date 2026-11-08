package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.mixin.SpreadingSnowyDirtBlockMixin;
import com.Gabou.sereneseasonsplus.tags.SSPTags;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class SnowVegetationRegressionTest {
    private static final BlockPos POS = new BlockPos(0, 64, 0);
    private final VanillaSnowBlockCompatibility compatibility = new VanillaSnowBlockCompatibility();

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.BLOCK.bindTags(Map.of(
            SSPTags.Blocks.SNOW_REPLACEABLE, List.of(Blocks.GRASS.builtInRegistryHolder(), Blocks.TALL_GRASS.builtInRegistryHolder()),
            BlockTags.DIRT, List.of(Blocks.DIRT.builtInRegistryHolder(), Blocks.GRASS_BLOCK.builtInRegistryHolder())
        ));
    }

    @Test
    public void onlyTaggedVegetationIsReplaceable() {
        assertTrue(compatibility.isReplaceableForSnow(Blocks.GRASS.defaultBlockState()));
        assertTrue(compatibility.isReplaceableForSnow(Blocks.TALL_GRASS.defaultBlockState()));
        for (Block block : List.of(Blocks.REDSTONE_WIRE, Blocks.RAIL, Blocks.DRAGON_HEAD, Blocks.DANDELION)) {
            assertFalse(block.toString(), compatibility.isReplaceableForSnow(block.defaultBlockState()));
        }
    }

    @Test
    public void precipitationSetterPreservesImportantBlocks() {
        ServerLevel level = mock(ServerLevel.class);
        for (Block block : List.of(Blocks.REDSTONE_WIRE, Blocks.RAIL, Blocks.DRAGON_HEAD)) {
            when(level.getBlockState(POS)).thenReturn(block.defaultBlockState());
            assertFalse(ServerPrecipitationService.setBlockAndTrackSnow(level, POS, Blocks.SNOW.defaultBlockState()));
        }
        verify(level, never()).setBlockAndUpdate(any(), any());
    }

    @Test
    public void staleSnowChangesCannotOverwritePlayerBlocks() {
        ServerLevel level = mock(ServerLevel.class);
        BlockState original = Blocks.SNOW.defaultBlockState();
        var mutation = SnowWorldMutation.setBlockIfUnchanged(POS, original, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        when(level.getBlockState(POS)).thenReturn(Blocks.RAIL.defaultBlockState());
        assertFalse(mutation.apply(level));
        verify(level, never()).setBlock(any(), any(), anyInt());
        when(level.getBlockState(POS)).thenReturn(original);
        when(level.setBlock(POS, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)).thenReturn(true);
        assertTrue(mutation.apply(level));
    }

    @Test
    public void staleTrackingCannotClearUnrelatedBlocks() {
        ServerLevel level = mock(ServerLevel.class);
        assertNull(compatibility.createClearMutation(level, POS, Blocks.DRAGON_HEAD.defaultBlockState(), false));
    }

    @Test
    public void tallGrassAndFlowersPlaceBothHalves() throws Exception {
        for (Block block : List.of(Blocks.TALL_GRASS, Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH)) {
            ServerLevel level = mock(ServerLevel.class);
            when(level.getMaxBuildHeight()).thenReturn(320);
            when(level.getBlockState(POS.below())).thenReturn(Blocks.DIRT.defaultBlockState());
            when(level.isEmptyBlock(POS.above())).thenReturn(true);
            placePlant(level, block);
            verify(level).setBlock(eq(POS), eq(block.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)), eq(Block.UPDATE_ALL));
            verify(level).setBlock(eq(POS.above()), eq(block.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)), eq(Block.UPDATE_ALL));
        }
    }

    @Test
    public void blockedUpperHalfPreventsGrowth() throws Exception {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getMaxBuildHeight()).thenReturn(320);
        when(level.getBlockState(POS.below())).thenReturn(Blocks.DIRT.defaultBlockState());
        when(level.isEmptyBlock(POS.above())).thenReturn(false);
        placePlant(level, Blocks.TALL_GRASS);
        verify(level, never()).setBlock(any(), any(), anyInt());
    }

    private static void placePlant(ServerLevel level, Block block) throws Exception {
        var method = SpreadingSnowyDirtBlockMixin.class.getDeclaredMethod("sereneseasonsplus$placeVegetation", ServerLevel.class, BlockPos.class, BlockState.class);
        method.setAccessible(true);
        method.invoke(null, level, POS, block.defaultBlockState());
    }
}

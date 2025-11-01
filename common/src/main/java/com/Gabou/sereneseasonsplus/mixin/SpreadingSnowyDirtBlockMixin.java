package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.tags.SSPTags;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class SpreadingSnowyDirtBlockMixin {

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void sereneseasonsplus$growVegetation(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if(!EnvironmentHelper.isGrassFloweringEnabled()) return;
        if(!EnvironmentHelper.isHotSeason()) return;

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir()) return;

        if (level.getMaxLocalRawBrightness(above) < 9) return;

        // only once in a while
        if (random.nextInt(EnvironmentHelper.getGrassChance()) != 0) return;


        if (random.nextFloat() < 0.8f) {
            // 80% chance: grow vanilla grass
            if (random.nextBoolean()) {
                level.setBlock(above, Blocks.GRASS.defaultBlockState(), 3);
            } else {
                level.setBlock(above, Blocks.TALL_GRASS.defaultBlockState(), 3);
            }
        } else {
            // 20% chance: grow a random flower
            var flowerTag = level.registryAccess()
                    .registryOrThrow(Registries.BLOCK)
                    .getTag(SSPTags.Blocks.FLOWERS);

            if (flowerTag.isPresent()) {
                List<Holder<Block>> flowers = flowerTag.get().stream().toList();
                if (!flowers.isEmpty()) {
                    Holder<Block> randomFlower = flowers.get(random.nextInt(flowers.size()));
                    Block flowerBlock = randomFlower.value();
                    level.setBlock(above, flowerBlock.defaultBlockState(), 3);
                    return;
                }
            }

            // fallback if tag not found or empty
            level.setBlock(above, Blocks.DANDELION.defaultBlockState(), 3);
        }
    }
}

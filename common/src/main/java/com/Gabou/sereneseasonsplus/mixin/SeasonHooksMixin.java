package com.Gabou.sereneseasonsplus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.init.ModConfig;
import sereneseasons.season.SeasonHooks;

@Mixin(SeasonHooks.class)
public abstract class SeasonHooksMixin {

    /**
     * Injects at RETURN instead of overwriting, preserving other mixins
     * (No Man’s Land, Serene Wild) while still replacing the final snow logic.
     */
    @Inject(method = "shouldSnowHook", at = @At("RETURN"), cancellable = true, remap = false)
    private static void shouldSnowHook(
            Biome biome, LevelReader levelReader, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        boolean vanillaResult = cir.getReturnValue();

        if (!ModConfig.seasons.generateSnowAndIce) {
            return;
        }

        boolean customLogic =
                !SeasonHooks.warmEnoughToRainSeasonal(levelReader, pos)
                        && pos.getY() >= levelReader.getMinBuildHeight()
                        && pos.getY() < levelReader.getMaxBuildHeight()
                        && levelReader.getBrightness(LightLayer.BLOCK, pos) < 10
                        && levelReader.canSeeSkyFromBelowWater(pos)
                        && Blocks.SNOW.defaultBlockState().canSurvive(levelReader, pos);

        if (customLogic != vanillaResult) {
            cir.setReturnValue(customLogic);
        }
    }
}

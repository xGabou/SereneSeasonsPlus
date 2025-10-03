package com.Gabou.sereneseasonsplus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import sereneseasons.season.SeasonHooks;
import sereneseasons.init.ModConfig;

@Mixin(SeasonHooks.class)
public abstract class SeasonHooksMixin {


    /**
     * @author Gabou
     * @reason Custom snow and ice generation conditions
     */
    @Overwrite(remap = false)
    public static boolean shouldSnowHook(Biome biome, LevelReader levelReader, BlockPos pos) {
        if ((!ModConfig.seasons.generateSnowAndIce || !SeasonHooks.warmEnoughToRainSeasonal(levelReader, pos))
                && (ModConfig.seasons.generateSnowAndIce || !biome.warmEnoughToRain(pos))) {

            if (pos.getY() >= levelReader.getMinBuildHeight()
                    && pos.getY() < levelReader.getMaxBuildHeight()
                    && levelReader.getBrightness(LightLayer.BLOCK, pos) < 10) {

                // Only check if snow can survive here, no air requirement
                if (Blocks.SNOW.defaultBlockState().canSurvive(levelReader, pos)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }
}

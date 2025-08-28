package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;

import java.util.HashMap;
import java.util.Map;

public class SnowUtils {

    private static final Logger LOGGER = LogManager.getLogger("SnowUtils");
    private static final Map<String, Float> biomeTemperatures = new HashMap();

    /**
     * Returns the biome temperature for the given position using a simple cache keyed by biome.
     * In winter sub-seasons, caps temperature at 0.14F for snow behavior.
     *
     * @param level the level
     * @param pos the position
     * @param currentSubSeason current sub-season
     * @return cached or computed biome base temperature
     */
    public static float getCachedBiomeTemperature(Level level, BlockPos pos, Season.SubSeason currentSubSeason) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        String biomeName = biomeHolder.unwrapKey().map(Object::toString).orElse("unknown");
        if (!biomeTemperatures.containsKey(biomeName)) {
            float temperature = getBiomeTemperature(biomeHolder);
            if (isWinterSubSeason(currentSubSeason) && temperature > 0.14F) {
                temperature = 0.14F;
            }

            biomeTemperatures.put(biomeName, temperature);
            LOGGER.info("Biome: {}, Temperature: {}", biomeName, temperature);
            return temperature;
        } else {
            float cachedTemperature = (Float)biomeTemperatures.get(biomeName);
            if (!isWinterSubSeason(currentSubSeason)) {
                float newTemperature = getBiomeTemperature(biomeHolder);
                if (newTemperature != cachedTemperature || cachedTemperature <= 0.14F) {
                    biomeTemperatures.put(biomeName, newTemperature);
                    LOGGER.info("Biome: {}, Updated Temperature: {}", biomeName, newTemperature);
                    return newTemperature;
                }
            }

            if (isWinterSubSeason(currentSubSeason) && cachedTemperature > 0.14F) {
                cachedTemperature = 0.14F;
                biomeTemperatures.put(biomeName, cachedTemperature);
                LOGGER.info("Biome: {}, Reset Temperature to Winter: {}", biomeName, cachedTemperature);
            }

            return cachedTemperature;
        }
    }
    /**
     * Extracts the base temperature from the given biome holder.
     *
     * @param biomeHolder biome holder
     * @return base temperature
     */
    public static float getBiomeTemperature(Holder<Biome> biomeHolder) {
        Biome biome = biomeHolder.value();
        return biome.getBaseTemperature();
    }
    /**
     * Whether the provided sub-season is part of winter.
     *
     * @param subSeason a sub-season
     * @return true if early/mid/late winter
     */
    private static boolean isWinterSubSeason(Season.SubSeason subSeason) {
        return subSeason == Season.SubSeason.EARLY_WINTER || subSeason == Season.SubSeason.MID_WINTER || subSeason == Season.SubSeason.LATE_WINTER;
    }

    /**
     * Decrement a layered block (e.g., SNOW) at the given pos. If it's at the minimum
     * layer (1) or not a layered block, remove it (set to AIR).
     * Uses update flags = 3 (neighbors + clients).
     *
     * @param level the world/level
     * @param pos   target position
     */
    public static void breakOrDecrementLayer(Level level, BlockPos pos) {
        breakOrDecrementLayer(level, pos, 3);
    }

    /**
     * Same as {@link #breakOrDecrementLayer(Level, BlockPos)} but with custom flags.
     */
    public static void breakOrDecrementLayer(Level level, BlockPos pos, int flags) {
        BlockState state = level.getBlockState(pos);

        if (state.hasProperty(BlockStateProperties.LAYERS)) {
            int layers = state.getValue(BlockStateProperties.LAYERS);
            if (layers > 1) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LAYERS, layers - 1), flags);
                return;
            }
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
    }
}

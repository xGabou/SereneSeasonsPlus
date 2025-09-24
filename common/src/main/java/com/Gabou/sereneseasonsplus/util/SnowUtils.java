package com.Gabou.sereneseasonsplus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
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
     * Returns a biome's temperature, cached by biome key, with a seasonal cap
     * applied in winter sub-seasons. The cache updates outside of winter or
     * when values change.
     *
     * @param level             level for biome lookup
     * @param pos               position of interest
     * @param currentSubSeason  current sub-season used to cap temperature
     * @return cached or freshly-computed biome temperature
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
            return temperature;
        } else {
            float cachedTemperature = (Float)biomeTemperatures.get(biomeName);
            if (!isWinterSubSeason(currentSubSeason)) {
                float newTemperature = getBiomeTemperature(biomeHolder);
                if (newTemperature != cachedTemperature || cachedTemperature <= 0.14F) {
                    biomeTemperatures.put(biomeName, newTemperature);
                    return newTemperature;
                }
            }

            if (isWinterSubSeason(currentSubSeason) && cachedTemperature > 0.14F) {
                cachedTemperature = 0.14F;
                biomeTemperatures.put(biomeName, cachedTemperature);
            }

            return cachedTemperature;
        }
    }

    /**
     * Extracts the base temperature from a biome holder.
     *
     * @param biomeHolder biome holder
     * @return base temperature reported by the biome
     */
    public static float getBiomeTemperature(Holder<Biome> biomeHolder) {
        Biome biome = biomeHolder.value();
        return biome.getBaseTemperature();
    }

    /**
     * Whether the sub-season is one of the winter sub-seasons.
     *
     * @param subSeason sub-season to test
     * @return true if early/mid/late winter
     */
    private static boolean isWinterSubSeason(Season.SubSeason subSeason) {
        return subSeason == Season.SubSeason.EARLY_WINTER || subSeason == Season.SubSeason.MID_WINTER || subSeason == Season.SubSeason.LATE_WINTER;
    }

    /**
     * Decrements a layered block (e.g., snow) if possible; otherwise removes
     * the block entirely by setting air.
     *
     * @param level level to modify
     * @param pos   target position
     */
    public static void breakOrDecrementLayer(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> doBreakOrDecrementLayer(serverLevel, pos));
        } else {
            doBreakOrDecrementLayer(level, pos);
        }
    }

    private static void doBreakOrDecrementLayer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.LAYERS)) {
            int layers = state.getValue(BlockStateProperties.LAYERS);
            if (layers > 1) {
                BlockState newState = state.setValue(BlockStateProperties.LAYERS, layers - 1);
                if (!newState.equals(state)) {
                    level.setBlock(
                            pos,
                            newState,
                            Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
                    );
                }
                return;
            }
        }
        if (!level.getBlockState(pos).is(Blocks.AIR)) {
            level.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
            );
        }
    }


}

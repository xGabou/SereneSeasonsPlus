package com.Gabou.sereneseasonsextended.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;

import java.util.HashMap;
import java.util.Map;

public class SnowUtils {

    private static final Logger LOGGER = LogManager.getLogger("SnowUtils");
    private static final Map<String, Float> biomeTemperatures = new HashMap();

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
    public static float getBiomeTemperature(Holder<Biome> biomeHolder) {
        Biome biome = biomeHolder.value();
        return biome.getBaseTemperature();
    }
    private static boolean isWinterSubSeason(Season.SubSeason subSeason) {
        return subSeason == Season.SubSeason.EARLY_WINTER || subSeason == Season.SubSeason.MID_WINTER || subSeason == Season.SubSeason.LATE_WINTER;
    }
}

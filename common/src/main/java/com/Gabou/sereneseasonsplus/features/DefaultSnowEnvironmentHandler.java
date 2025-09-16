package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.features.snowstorm.IWeatherChunk;
import com.Gabou.sereneseasonsplus.features.snowstorm.WeatherState;
import com.Gabou.sereneseasonsplus.storage.Priority;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import static com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature.*;

public class DefaultSnowEnvironmentHandler implements SnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

        if (temperature >= 0.15F) {
            return CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
        }
        return 0;
    }

    @Override
    public WeatherDecision decideWeatherAction(ServerLevel level, Season.SubSeason sub, float temperature, boolean coldEnoughToSnow, BlockPos pos) {
        // Treat snow as the highest priority outcome
        // Suggest using 0.15F to match vanilla coldEnoughToSnow threshold
        final boolean isPrecip = level.isRaining();
        final boolean wantsSnow = isPrecip && coldEnoughToSnow;

        Action action = Action.NONE;
        Priority priority = Priority.GRADUAL;

        if (wantsSnow) {
            // Do not let seasonal melt rules override actual snowfall
            return new WeatherDecision(Action.SNOW, Priority.GRADUAL);
        }

        switch (sub) {
            case MID_SPRING, MID_AUTUMN -> {
                if (temperature >= 0.5F) {
                    action = Action.MELT;
                    priority = Priority.ACCELERATED;
                } else if (temperature >= 0.15F) {
                    action = Action.MELT;
                    priority = Priority.ACCELERATED;
                }
            }
            case LATE_SPRING, EARLY_AUTUMN, EARLY_SUMMER, MID_SUMMER, LATE_SUMMER -> {
                if (temperature >= 0.5F) {
                    action = Action.CLEAR;
                    priority = Priority.URGENT;
                } else if (temperature >= 0.15F) {
                    action = Action.MELT;
                    priority = Priority.GRADUAL;
                }
            }
            case EARLY_SPRING, LATE_AUTUMN -> {
                if (temperature >= 0.15F) {
                    action = Action.MELT;
                    priority = Priority.GRADUAL;
                }
            }
            default -> { /* keep NONE + GRADUAL */ }
        }

        return new WeatherDecision(action, priority);
    }

}

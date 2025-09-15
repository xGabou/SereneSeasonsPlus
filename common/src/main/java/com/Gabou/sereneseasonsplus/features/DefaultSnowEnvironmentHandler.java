package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.features.snowstorm.IWeatherChunk;
import com.Gabou.sereneseasonsplus.features.snowstorm.WeatherState;
import com.Gabou.sereneseasonsplus.storage.Priority;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
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
    public WeatherDecision decideWeatherAction(ServerLevel level, Season.SubSeason sub, float temperature) {
        // Default = snowfall (if precipitating), gradual
        boolean isRaining = level.isRaining();
        Action action = isRaining && temperature < 0.5F ? Action.SNOW : Action.NONE;
        Priority priority = Priority.GRADUAL;

        switch (sub) {
            case MID_SPRING, MID_AUTUMN -> {
                if (temperature >= 0.15F && temperature < 0.5F) {
                    action = Action.MELT;
                    priority = Priority.ACCELERATED;
                } else if (temperature >= 0.5F) {
                    action = Action.MELT;
                    priority = Priority.ACCELERATED;
                } else if (isRaining) {
                    action = Action.SNOW;
                    priority = Priority.GRADUAL;
                }
            }
            case LATE_SPRING, EARLY_AUTUMN,
                 EARLY_SUMMER, MID_SUMMER, LATE_SUMMER -> {
                if (temperature >= 0.5F) {
                    action = Action.CLEAR;
                    priority = Priority.URGENT;
                } else if (temperature >= 0.15F) {
                    action = Action.MELT;
                    priority = Priority.GRADUAL;
                } else if (isRaining) {
                    action = Action.SNOW;
                    priority = Priority.GRADUAL;
                }
            }
            case EARLY_SPRING, LATE_AUTUMN -> {
                if (temperature >= 0.15F) {
                    action = Action.MELT;
                    priority = Priority.GRADUAL;
                } else if (isRaining) {
                    action = Action.SNOW;
                    priority = Priority.GRADUAL;
                }
            }
            default -> {
                if (isRaining && temperature < 0.5F) {
                    action = Action.SNOW;
                    priority = Priority.GRADUAL;
                }
            }
        }


        return new WeatherDecision(action, priority);
    }
}

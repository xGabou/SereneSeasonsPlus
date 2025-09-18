package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.storage.Priority;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;

import static com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature.*;

public class NeoForgeSnowEnvironmentHandler extends DefaultSnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        if (!SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded) {
            Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
            float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

            if (temperature >= 0.15F) {
                return CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
            }
        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos),
                    level.getDayTime()
            );

            if (temperature >= 0.5F) {
                return CommonSnowBlockFeature.calculateBlocksToReplace1(temperature);
            }
        }
        return 0;
    }

    @Override
    public WeatherDecision decideWeatherAction(ServerLevel level, Season.SubSeason sub, float temperature, boolean coldEnoughToSnow, BlockPos pos) {
        if(!SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded)
        {
            return super.decideWeatherAction(level, sub, temperature, coldEnoughToSnow, pos);
        }
        temperature =ForecastOrchestrator.getCurrentTemperature(
                new BiomeInstanceKey(level.getBiome(pos).unwrapKey().get().location(), pos),
                level.getDayTime()
        );

        final boolean isPrecip = EnvironmentHelper.isRainning(level, pos);
        final boolean wantsSnow = isPrecip && temperature < 0.1F;

        Action action = Action.NONE;
        Priority priority = Priority.GRADUAL;

        // Snowfall always overrides melting logic
        if (wantsSnow) {
            return new WeatherDecision(Action.SNOW, Priority.GRADUAL);
        }

        // Celsius thresholds
        // ≥ 10 °C: completely clear snow
        // ≥ 0.5 °C: melt (accelerated in warm seasons, gradual otherwise)
        // < 0.5 °C: do nothing unless snowing
        switch (sub) {
            case MID_SPRING, MID_AUTUMN, LATE_SPRING, EARLY_AUTUMN, EARLY_SUMMER, MID_SUMMER, LATE_SUMMER -> {
                if (temperature >= 10.0F) {
                    action = Action.CLEAR;
                    priority = Priority.URGENT;
                } else if (temperature >= 0.5F) {
                    action = Action.MELT;
                    priority = Priority.ACCELERATED;
                }
            }
            case EARLY_SPRING, LATE_AUTUMN -> {
                if (temperature >= 0.5F) {
                    action = Action.MELT;
                    priority = Priority.GRADUAL;
                }
            }
            default -> {
                if (temperature >= 10.0F) {
                    action = Action.CLEAR;
                    priority = Priority.URGENT;
                } else if (temperature >= 0.5F) {
                    action = Action.MELT;
                    priority = Priority.GRADUAL;
                }
            }
        }

        return new WeatherDecision(action, priority);
    }



}
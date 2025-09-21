package com.Gabou.sereneseasonsplus.features;
import com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;

public class ForgeSnowEnvironmentHandler extends DefaultSnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        if (!SereneSeasonsPlusForge.isProjectAtmosphereLoaded) {
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
        return -level.random.nextInt(2,6);
    }
}

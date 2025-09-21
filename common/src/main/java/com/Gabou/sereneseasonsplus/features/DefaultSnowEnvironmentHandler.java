package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

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
}

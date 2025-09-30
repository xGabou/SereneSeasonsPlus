package com.Gabou.sereneseasonsplus.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class FabricEnvironmentHelper implements IEnvironmentHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricEnvironmentHelper");
    private Season.SubSeason season;
    private boolean isHotSeason;
    private boolean isSnowySeason;

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    @Override
    public boolean shouldRunMod() {
        return !isClient();
    }

    @Override
    public boolean isHotSeason() {
        return isHotSeason;
    }

    @Override
    public boolean isSnowySeason() {
        return isSnowySeason;
    }

    @Override
    public Season.SubSeason getCurrentSeason() {
        return season;
    }

    @Override
    public void onSeasonChange(ServerLevel serverLevel) {
        season = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        LOGGER.info("Season changed to: {}", season);
        isHotSeason = HotSeason.isHotSeason(season);
        isSnowySeason = SnowySeason.isSnowySeason(season);
    }

    /**
     * @param level
     * @param pos
     * @return
     */
    @Override
    public boolean isRainingAt(ServerLevel level, BlockPos pos) {
        return level.isRaining();
    }
}

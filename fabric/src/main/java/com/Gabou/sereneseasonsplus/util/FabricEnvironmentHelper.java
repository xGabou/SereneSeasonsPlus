package com.Gabou.sereneseasonsplus.util;

import net.fabricmc.loader.api.FabricLoader;
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
    private int baseChance = -1;

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
        baseChance = getGrassChance(true);
    }
    @Override
    public int getGrassChance(boolean force) {
        if(baseChance != -1 || force) {
            return baseChance;
        }
        switch (season) {
            case EARLY_SUMMER, LATE_SUMMER -> baseChance = 300; // faster
            case MID_SUMMER -> baseChance = 200;   // fastest
            case EARLY_SPRING, LATE_AUTUMN ->  baseChance = 1200; // slowest
            case MID_SPRING, MID_AUTUMN -> baseChance = 800;    // slower
            case LATE_SPRING, EARLY_AUTUMN -> baseChance = 600;   // slow
        }

        return baseChance;
    }
}

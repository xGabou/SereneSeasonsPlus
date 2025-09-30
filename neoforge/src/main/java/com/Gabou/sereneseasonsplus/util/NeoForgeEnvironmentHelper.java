package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import net.Gabou.projectatmosphere.api.AtmoApi;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class NeoForgeEnvironmentHelper implements IEnvironmentHelper{
    private static final Logger LOGGER = LogManager.getLogger("EnvironmentHelper");


    private static Season.SubSeason season;

    private static boolean isHotSeason = false;

    private boolean isSnowySeason;

    /**
     * Indicates whether the current sub-season is considered hot.
     *
     * @return true if hot season is active
     */
    @Override
    public boolean isHotSeason() {
        return isHotSeason;
    }



    @Override
    public boolean isSnowySeason() {
        return isSnowySeason;
    }

    /**
     * Returns the cached current sub-season.
     *
     * @return current sub-season, may be null before first update
     */
    @Override
    public Season.SubSeason getCurrentSeason() {
        return season;
    }

    /**
     * Whether the mod logic should run in the current environment.
     *
     * @return true if running on a dedicated server or single-player host
     */
    @Override
    public boolean shouldRunMod() {
        return FMLEnvironment.dist.isDedicatedServer()
                || (FMLEnvironment.dist.isClient() && Minecraft.getInstance().hasSingleplayerServer());
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    /**
     * Updates cached season information based on the given server level.
     *
     * @param serverLevel server level where season changed
     */
    @Override
    public void onSeasonChange(ServerLevel serverLevel) {

        season = SeasonHelper.getSeasonState(serverLevel).getSubSeason();
        LOGGER.info("Season changed to: {}", season);
        isHotSeason = HotSeason.isHotSeason(season);
        isSnowySeason = SnowySeason.isSnowySeason(season);
        // Proactively update snow/ice state in loaded chunks around players
        CommonSnowBlockFeature.onSeasonChange(serverLevel);
    }

    @Override
    public boolean isRainingAt(ServerLevel level, BlockPos pos) {
        if(SereneSeasonsPlusNeoForge.isProjectAtmosphereLoaded)
            return level.isRainingAt(pos);
        else
            return level.isRaining();
    }


}

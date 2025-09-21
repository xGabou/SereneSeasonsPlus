package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge;
import net.Gabou.projectatmosphere.ProjectAtmosphere;
import net.Gabou.projectatmosphere.api.AtmoApi;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class ForgeEnvironmentHelper implements IEnvironmentHelper {
    private static final Logger LOGGER = LogManager.getLogger("ForgeEnvironmentHelper");
    private Season.SubSeason season;
    private boolean isHotSeason;

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean shouldRunMod() {
        return FMLEnvironment.dist.isDedicatedServer()
                || (FMLEnvironment.dist.isClient() && Minecraft.getInstance().hasSingleplayerServer());
    }

    @Override
    public boolean isHotSeason() {
        return isHotSeason;
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

    }

    @Override
    public boolean isRainning(ServerLevel level, BlockPos pos) {
        if(SereneSeasonsPlusForge.isProjectAtmosphereLoaded)
            return AtmoApi.getInstance().isRainningAt(level, pos);
        return level.isRaining();
    }
}

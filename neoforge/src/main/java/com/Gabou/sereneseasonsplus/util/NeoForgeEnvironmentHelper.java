package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import net.Gabou.projectatmosphere.api.AtmoApi;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.Gabou.gaboulibs.util.SnowySeason;

public class NeoForgeEnvironmentHelper implements IEnvironmentHelper{
    private static final Logger LOGGER = LogManager.getLogger("EnvironmentHelper");


    private static Season.SubSeason season;

    private static boolean isHotSeason = false;

    private boolean isSnowySeason;
    private int baseChance = -1;

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
    /**
     * @return if Serene Wild is loaded
     */
    @Override
    public boolean isSereneWildLoaded() {
        return ModList.get().isLoaded("serenewild");
    }

    /**
     * @param modId the mod id
     * @return if the mod is loaded
     */
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * @return if Snow Real Magic is loaded
     */
    @Override
    public boolean isSnowRealMagicLoaded() {
        return ModList.get().isLoaded("snowrealmagic");
    }


    @Override
    public boolean isGrassFloweringEnabled() {
        return SereneExtendedConfig.GRASS_FLOWER_GROWTH_ENABLED.get();
    }


}

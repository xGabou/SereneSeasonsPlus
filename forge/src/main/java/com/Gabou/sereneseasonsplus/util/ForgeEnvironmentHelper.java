package com.Gabou.sereneseasonsplus.util;


import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.Gabou.gaboulibs.util.SnowySeason;



public class ForgeEnvironmentHelper implements IEnvironmentHelper {
    private static final Logger LOGGER = LogManager.getLogger("ForgeEnvironmentHelper");
    private Season.SubSeason season;
    private boolean isHotSeason;
    private int baseChance = -1;

    private boolean isSnowySeason;

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

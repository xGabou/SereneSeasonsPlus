package com.Gabou.sereneseasonsplus.util;

import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;

public interface IEnvironmentHelper {
    boolean isClient();
    boolean shouldRunMod();
    boolean isHotSeason();
    boolean isSnowySeason();
    Season.SubSeason getCurrentSeason();
    void onSeasonChange(ServerLevel serverLevel);
}

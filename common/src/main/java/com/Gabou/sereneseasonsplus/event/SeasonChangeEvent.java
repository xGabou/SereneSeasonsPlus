package com.Gabou.sereneseasonsplus.event;

import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import glitchcore.event.EventManager;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonChangedEvent;

public class SeasonChangeEvent {

    /**
     * Subscribes to Serene Seasons events and forwards season changes to the
     * environment helper so the mod can react (e.g., recompute hot season state).
     */
    public static void register() {
        EventManager.addListener((SeasonChangedEvent.Standard event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                       Season.SubSeason oldSeason = event.getPrevSeason();
                Season.SubSeason newSeason = event.getNewSeason();
                if (newSeason != oldSeason) {
                    EnvironmentHelper.onSeasonChange(serverLevel,Math.abs(newSeason.ordinal() - oldSeason.ordinal()) != 1);
                }

            }
        });

        EventManager.addListener((SeasonChangedEvent.Tropical event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                Season.TropicalSeason oldSeason = event.getPrevSeason();
                Season.TropicalSeason newSeason = event.getNewSeason();
                if (newSeason != oldSeason) {
                    EnvironmentHelper.onSeasonChange(serverLevel,Math.abs(newSeason.ordinal() - oldSeason.ordinal()) != 1);
                }
            }
        });
    }
}

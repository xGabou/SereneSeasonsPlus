package com.Gabou.sereneseasonsplus.event;

import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import glitchcore.event.EventManager;
import net.Gabou.projectatmosphere.manager.AtmosphereManager;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.SeasonChangedEvent;

public class SeasonChangeEvent {

    /**
     * TODO: describe method.
     */
    public static void register() {
        EventManager.addListener((SeasonChangedEvent.Standard event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                if (event.getNewSeason().getSeason() != event.getPrevSeason().getSeason()) {
                    EnvironmentHelper.onSeasonChange(serverLevel);
                }
            }
        });

        EventManager.addListener((SeasonChangedEvent.Tropical event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                if (event.getNewSeason() != event.getPrevSeason()) {
                    EnvironmentHelper.onSeasonChange(serverLevel);
                }
            }
        });
    }
}

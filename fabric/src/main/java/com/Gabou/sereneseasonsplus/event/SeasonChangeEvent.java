package com.Gabou.sereneseasonsplus.event;

import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.FabricEnvironmentHelper;
import glitchcore.event.EventManager;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.SeasonChangedEvent;

public class SeasonChangeEvent {

    /**
     * Subscribes to Serene Seasons events and forwards season changes to the
     * environment helper so the mod can react (e.g., recompute hot season state).
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

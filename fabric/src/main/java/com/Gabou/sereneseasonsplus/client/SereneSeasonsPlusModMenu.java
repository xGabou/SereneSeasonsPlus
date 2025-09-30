package com.Gabou.sereneseasonsplus.client;

import com.Gabou.sereneseasonsplus.client.config.SereneExtendedScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class SereneSeasonsPlusModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SereneExtendedScreen::new;
    }
}

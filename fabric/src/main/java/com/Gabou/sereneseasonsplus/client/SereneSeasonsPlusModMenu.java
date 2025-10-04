package com.Gabou.sereneseasonsplus.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.Gabou.sereneseasonsplus.client.config.SereneExtendedScreen;

public class SereneSeasonsPlusModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SereneExtendedScreen::new;
    }
}

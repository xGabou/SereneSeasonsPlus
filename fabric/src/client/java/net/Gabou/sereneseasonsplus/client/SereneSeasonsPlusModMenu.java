package net.Gabou.sereneseasonsplus.client;

import net.Gabou.sereneseasonsplus.client.config.SereneExtendedScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class SereneSeasonsPlusModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> new SereneExtendedScreen(parent);
    }
}

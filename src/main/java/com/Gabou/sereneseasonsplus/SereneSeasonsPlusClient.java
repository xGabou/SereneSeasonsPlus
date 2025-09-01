package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;

public class SereneSeasonsPlusClient {

    /**
     * Registers the configuration screen factory for the client.
     */
    public SereneSeasonsPlusClient(ModLoadingContext modContainer) {
        modContainer.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new SereneExtendedScreen(parent)
                )
        );
    }
}

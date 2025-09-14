package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;

public class SereneSeasonsPlusNeoForgeClient {

    /**
     * Registers the configuration screen factory for the client.
     */
    public SereneSeasonsPlusNeoForgeClient(ModLoadingContext modContainer) {
        modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SereneExtendedScreen(screen)));
    }
}

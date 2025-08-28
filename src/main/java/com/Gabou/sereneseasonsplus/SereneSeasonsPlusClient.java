package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

public class SereneSeasonsPlusClient {
    public static void init(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SereneExtendedScreen(screen)));
    }
}

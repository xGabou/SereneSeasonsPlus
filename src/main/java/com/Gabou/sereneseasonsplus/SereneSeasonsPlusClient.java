package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class SereneSeasonsPlusClient {
    public static void init() {
        FMLJavaModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SereneExtendedScreen(screen)));
    }
}

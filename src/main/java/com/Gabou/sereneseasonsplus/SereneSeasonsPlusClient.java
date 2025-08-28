package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class SereneSeasonsPlusClient {
    public SereneSeasonsPlusClient(ModContainer container) {
        // Register config screen for NeoForge 1.21.x
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            new java.util.function.Supplier<IConfigScreenFactory>() {
                @Override
                public IConfigScreenFactory get() {
                    return new IConfigScreenFactory() {
                        @Override
                        public Screen createScreen(ModContainer c, Screen parent) {
                            return new SereneExtendedScreen(parent);
                        }
                    };
                }
            }
        );
    }
}

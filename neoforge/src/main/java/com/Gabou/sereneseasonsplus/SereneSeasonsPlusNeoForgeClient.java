package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import com.Gabou.sereneseasonsplus.client.PerformanceWarning;
import com.Gabou.sereneseasonsplus.client.ConfigResetWarning;
import com.Gabou.sereneseasonsplus.config.ConfigResetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@OnlyIn(Dist.CLIENT)
public class SereneSeasonsPlusNeoForgeClient {

    /**
     * Registers the configuration screen factory for the client.
     */
    public static void init(ModContainer context) {
        context.registerExtensionPoint(
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
    private static boolean shown = false;
    private static boolean configResetWarningHandled = false;


    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (!configResetWarningHandled) {
                if (ConfigResetManager.isWarningPending(FMLPaths.CONFIGDIR.get())) {
                    if (mc.screen == null) {
                        mc.setScreen(new ConfigResetWarning(FMLPaths.CONFIGDIR.get()));
                        configResetWarningHandled = true;
                    }
                    return;
                }
                configResetWarningHandled = true;
            }
            if (!shown && !PerfChecker.hasPerfMod()) {
                if (mc.screen == null) { // wait until no other screen is open
                    mc.setScreen(new PerformanceWarning());
                    shown = true;
                }
            }

    }
}


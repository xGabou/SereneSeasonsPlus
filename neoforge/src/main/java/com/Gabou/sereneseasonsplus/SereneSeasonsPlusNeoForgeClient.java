package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import com.Gabou.sereneseasonsplus.util.PerformanceWarning;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@OnlyIn(Dist.CLIENT)
public class SereneSeasonsPlusNeoForgeClient {

    /**
     * Registers the configuration screen factory for the client.
     */
    public static void init(ModLoadingContext context) {
        context.registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (mc, parent) -> new SereneExtendedScreen(parent)
        );
    }
    private static boolean shown = false;


    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
            if (!shown && !PerfChecker.hasPerfMod()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) { // wait until no other screen is open
                    mc.setScreen(new PerformanceWarning());
                    shown = true;
                }
            }

    }
}


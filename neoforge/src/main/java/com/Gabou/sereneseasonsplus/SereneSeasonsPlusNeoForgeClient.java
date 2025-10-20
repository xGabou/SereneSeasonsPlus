package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import com.Gabou.sereneseasonsplus.util.PerformanceWarning;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.event.TickEvent;

@OnlyIn(Dist.CLIENT)
public class SereneSeasonsPlusNeoForgeClient {


    private static boolean shown = false;


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (!shown && !PerfChecker.hasPerfMod()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) { // wait until no other screen is open
                    mc.setScreen(new PerformanceWarning());
                    shown = true;
                }
            }
        }
    }

    /**
     * Registers the configuration screen factory for the client.
     */
    public static void init(ModLoadingContext modContainer) {
        modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SereneExtendedScreen(screen)));
    }
}
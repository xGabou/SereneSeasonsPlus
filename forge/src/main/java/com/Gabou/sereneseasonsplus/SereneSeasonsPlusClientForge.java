package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.client.PerformanceWarning;
import com.Gabou.sereneseasonsplus.client.ConfigResetWarning;
import com.Gabou.sereneseasonsplus.config.ConfigResetManager;
import com.Gabou.sereneseasonsplus.config.SereneExtendedScreen;
import com.Gabou.sereneseasonsplus.util.PerfChecker;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@OnlyIn(Dist.CLIENT)
public class SereneSeasonsPlusClientForge {
    /**
     * Registers the mod config screen with Forge's extension point so it can
     * be opened from the Mods list in-game.
     *
     * @param context mod loading context used to register the screen factory
     */
    public static void init(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SereneExtendedScreen(screen)));
    }

    private static boolean shown = false;
    private static boolean configResetWarningHandled = false;


    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
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
}

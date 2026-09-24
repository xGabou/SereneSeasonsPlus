package com.Gabou.sereneseasonsplus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import com.Gabou.sereneseasonsplus.config.ConfigResetManager;

public class  SereneSeasonsPlusClient implements ClientModInitializer {

    private static boolean shown = false;
    private static boolean configResetWarningHandled = false;
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!configResetWarningHandled) {
                if (ConfigResetManager.isWarningPending(FabricLoader.getInstance().getConfigDir())) {
                    if (client.screen == null) {
                        client.setScreen(new ConfigResetWarning(FabricLoader.getInstance().getConfigDir()));
                        configResetWarningHandled = true;
                    }
                    return;
                }
                configResetWarningHandled = true;
            }
            if (!shown && !PerfChecker.hasPerfMod()) {
                if (client.screen == null) { // wait until no other screen is open
                    client.setScreen(new PerformanceWarning());
                    shown = true;
                }
            }
        });
    }
}

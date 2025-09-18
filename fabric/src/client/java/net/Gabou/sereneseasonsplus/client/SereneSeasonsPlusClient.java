package net.Gabou.sereneseasonsplus.client;

import com.Gabou.sereneseasonsplus.util.PerformanceWarning;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class  SereneSeasonsPlusClient implements ClientModInitializer {

    private static boolean shown = false;
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!shown && !PerfChecker.hasPerfMod()) {
                if (client.screen == null) { // wait until no other screen is open
                    client.setScreen(new PerformanceWarning());
                    shown = true;
                }
            }
        });
    }
}

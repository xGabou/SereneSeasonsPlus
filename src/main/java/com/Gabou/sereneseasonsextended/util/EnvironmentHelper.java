//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.Gabou.sereneseasonsextended.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnvironmentHelper {
    private static final Logger LOGGER = LogManager.getLogger("EnvironmentHelper");
    private static boolean isServerEnvironment;
    private static boolean isSinglePlayer;

    public static void initialize() {
        isServerEnvironment = !FMLEnvironment.dist.isClient();
        isSinglePlayer = !isServerEnvironment && detectSinglePlayer();
        LOGGER.info("Environment: Server = {}, Single Player = {}", isServerEnvironment, isSinglePlayer);
    }

    public static boolean shouldRunMod() {
        return FMLEnvironment.dist.isDedicatedServer()
                || (FMLEnvironment.dist.isClient() && Minecraft.getInstance().hasSingleplayerServer());
    }


    private static boolean detectSinglePlayer() {
        try {
            Minecraft mcInstance = Minecraft.getInstance();
            return mcInstance.hasSingleplayerServer();
        } catch (Exception e) {
            LOGGER.warn("Failed to determine single-player mode: {}", e.getMessage());
            return false;
        }
    }
}

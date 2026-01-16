package com.Gabou.sereneseasonsplus.client;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForgeClient;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature.LOGGER;

public final class SereneSeasonsPlusNeoForgeClientEntrypoint {
    private SereneSeasonsPlusNeoForgeClientEntrypoint() {}

    public static final Logger LOGGER = LogManager.getLogger("SereneSeasonsPlusNeoForgeClientEntrypoint");
    public static void init(ModContainer modContainer) {
        LOGGER.info("Setting up Serene Seasons Plus (Client)");
        SereneSeasonsPlusNeoForgeClient.init(modContainer);
        NeoForge.EVENT_BUS.register(SereneSeasonsPlusNeoForgeClient.class);
    }
}

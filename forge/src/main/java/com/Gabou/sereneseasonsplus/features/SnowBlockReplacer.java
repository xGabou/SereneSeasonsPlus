




package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ForgeEnvironmentHelper;

import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

@EventBusSubscriber
public class SnowBlockReplacer extends CommonSnowBlockReplacer {



    /**
     * Initializes tick thresholds and clears per-player position cache on server start.
     *
     * @param event server starting event
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        tickCounter = 0;
        playerPositions.clear();
        LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
    }


    /**
     * Refreshes the tick threshold and async config on server tick,
     * allowing live config changes.
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public static void onConfigReload(TickEvent.ServerTickEvent event) {
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        SereneService.reloadConfig();

    }

    /**
     * Periodically removes snow blocks around players when temperature is
     * above threshold. Offloads work to an async executor.
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END && EnvironmentHelper.shouldRunMod()) {
            handleServerTick( event.getServer());
        }
    }




    /**
     * Handles snow and ice in chunks when they are loaded based on temperature.
     * Extremely warm chunks have all snow removed immediately. Borderline warm
     * chunks have their snow layers reduced and are queued for gradual melting.
     *
     * @param event chunk load event
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: non-LevelChunk, skipping");
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: client side, skipping");
            return;
        }

        handleOnChunkLoad( level, chunk);
    }









}

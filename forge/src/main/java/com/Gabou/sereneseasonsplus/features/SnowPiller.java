package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ForgeEnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SnowPiller extends CommonSnowPiller {

    private static final Logger LOGGER = LogUtils.getLogger();




    /**
     * Utility holder; not instantiable.
     */
    private SnowPiller() {
    }


    /**
     * Initializes tick thresholds and per-player throttle state on server start.
     *
     * @param event server starting event
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        tickThresholdSnowPiller = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        tickCounter = 0;
        THROTTLE.clear();
    }

    /**
     * Refreshes the tick threshold and async config on server tick,
     * allowing live config changes.
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public static void onConfigReload(TickEvent.ServerTickEvent event) {
        tickThresholdSnowPiller = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        SereneService.reloadConfig();

    }

    /**
     * Periodically attempts to place snow around players if the environment
     * and config permit. Work is offloaded to an async executor.
     *
     * @param event server tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && EnvironmentHelper.shouldRunMod()) {
            MinecraftServer server = event.getServer();
            Level level = server.getLevel(Level.OVERWORLD);
            handleServerTick(level);
        }

    }









}

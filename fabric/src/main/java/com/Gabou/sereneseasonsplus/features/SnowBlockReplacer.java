package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.FabricEnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SnowBlockReplacer extends CommonSnowBlockReplacer{
    public static SnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger("sereneseasonsplus/SnowBlockReplacer");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> playerPositions = new HashMap<>();

    private static int tickThresholdSnowReplacer;
    private static final int tickThresholdSnowReplacerForHotSeasons = 30;
    private static int tickCounter = 0;

    public static void registerEvents() {
        // Server starting
        ServerLifecycleEvents.SERVER_STARTING.register(SnowBlockReplacer::onServerStarting);

        // Server tick
        ServerTickEvents.END_SERVER_TICK.register(SnowBlockReplacer::onServerTick);
    }

    private static void onServerStarting(MinecraftServer server) {
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        tickCounter = 0;
        playerPositions.clear();
        LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        Level level = server.getLevel(Level.OVERWORLD);
        if (level != null && !level.isClientSide()) {
            if (tickCounter % tickThresholdSnowReplacer == 0
                    || (tickCounter % tickThresholdSnowReplacerForHotSeasons == 0 && EnvironmentHelper.isHotSeason())) {
                updatePlayerPositions(server.getPlayerList().getPlayers());
                SereneService.runAsync(() -> replaceSnowBlocks(level));
            }
        }
    }

    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }
    }

    private static void replaceSnowBlocks(Level level) {
        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            int simulationDistance = getSimulationDistance(player);
            int radius = Mth.clamp(simulationDistance * 16, 16, 64);
            int blocksToReplace;
            Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
            float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);
            if (temperature < 0.15F) continue;
            blocksToReplace = calculateBlocksToReplace(temperature);
            for (int i = 0; i < blocksToReplace; i++) {
                BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                if (targetPos == null) break;
                SnowUtils.breakOrDecrementLayer(level, targetPos);
            }
        }
    }

    private static int calculateBlocksToReplace1(float temperature) {
        return (int) Math.ceil(temperature / 5.0F);
    }

    private static int calculateBlocksToReplace(float temperature) {
        if (temperature < 0.2F) return 1;
        else return (temperature < 0.5F) ? 3 : 5;
    }

    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }

    private static BlockPos findSnowBlockInRadius(Level level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.SNOW_BLOCK)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}

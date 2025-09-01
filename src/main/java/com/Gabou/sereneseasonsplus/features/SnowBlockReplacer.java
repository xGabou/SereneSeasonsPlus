package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

@Mod.EventBusSubscriber
public class SnowBlockReplacer {
    private static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> playerPositions = new HashMap();

    private static int tickThresholdSnowReplacer;
    private static int tickThresholdSnowReplacerForHotSeasons = 30;
    private static int tickCounter = 0;


    @SubscribeEvent
    /**
     * Initializes thresholds and clears state when the server starts.
     *
     * @param event server starting event
     */
    public static void onServerStarting(ServerStartingEvent event) {
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        tickCounter = 0;
        playerPositions.clear();
        LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
    }


    @SubscribeEvent
    /**
     * Refreshes configuration values periodically so new settings apply.
     *
     * @param event server post-tick event
     */
    public static void onConfigReload(TickEvent.ServerTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.END)) return;
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        SereneService.reloadConfig();

    }

    @SubscribeEvent
    /**
     * On server tick, schedules asynchronous snow block replacement
     * near players based on temperature and season.
     *
     * @param event server post-tick event
     */
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.END)) return;
        if (EnvironmentHelper.shouldRunMod()) {
            ++tickCounter;
            MinecraftServer server = event.getServer();
            Level level = server.getLevel(Level.OVERWORLD);
            if (level != null && !level.isClientSide()) {
                if (tickCounter % tickThresholdSnowReplacer == 0 || (tickCounter % tickThresholdSnowReplacerForHotSeasons == 0) && EnvironmentHelper.isHotSeason()) {
                    updatePlayerPositions(server.getPlayerList().getPlayers());
                    SereneService.runAsync(() -> replaceSnowBlocks(level));
                }

            }
        }
    }

    /**
     * Caches the current block positions for the given players.
     *
     * @param players iterable of players
     */
    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }

    }

    /**
     * Scans around tracked players and replaces snow blocks based on temperature.
     *
     * @param level the level to operate in
     */
    private static void replaceSnowBlocks(Level level) {
        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            int simulationDistance = getSimulationDistance(player);
            int radius = Mth.clamp(simulationDistance * 16, 16, 64);

            Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
            float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

            if (temperature >= 0.15F) { // warm enough to melt
                int blocksToReplace = calculateBlocksToReplace(temperature);

                for (int i = 0; i < blocksToReplace; ++i) {
                    BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                    if (targetPos == null) {
                        break;
                    }
                    SnowUtils.breakOrDecrementLayer(level, targetPos);
                }
            }
        }
    }



    /**
     * Determines how many snow blocks to replace depending on temperature.
     *
     * @param temperature biome temperature near player
     * @return number of blocks to replace this pass
     */
    private static int calculateBlocksToReplace(float temperature) {
        if (temperature < 0.2F) {
            return 1;
        } else {
            return temperature < 0.5F ? 3 : 5;
        }
    }

    /**
     * Returns the current simulation/view distance used for estimating radius.
     *
     * @param player the player
     * @return view distance in chunks
     */
    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }

    /**
     * Finds a nearby snow block position within a radius around a center.
     *
     * @param level  level to search
     * @param center center position
     * @param radius search radius
     * @return the first matching position or {@code null}
     */
    private static BlockPos findSnowBlockInRadius(Level level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                for (int y = -5; y <= 5; ++y) {
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

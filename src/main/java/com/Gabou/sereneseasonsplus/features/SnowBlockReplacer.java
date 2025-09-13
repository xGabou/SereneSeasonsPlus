




package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

@EventBusSubscriber
public class SnowBlockReplacer {
    private static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> playerPositions = new HashMap();
    private static final Set<ChunkPos> meltingChunks = new HashSet();

    private static int tickThresholdSnowReplacer;
    private static int tickThresholdSnowReplacerForHotSeasons = 30;
    private static int tickCounter = 0;


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
            ++tickCounter;
            MinecraftServer server = event.getServer();
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level != null && !level.isClientSide()) {
                if (tickCounter % tickThresholdSnowReplacer == 0 || (tickCounter % tickThresholdSnowReplacerForHotSeasons == 0) && EnvironmentHelper.isHotSeason()) {
                    updatePlayerPositions(server.getPlayerList().getPlayers());
                    SereneService.runAsync(() -> replaceSnowBlocks(level));
                    if (!meltingChunks.isEmpty()) {
                        SereneService.runAsync(() -> processMeltingChunks(level));
                    }
                }

            }
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
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        BlockPos worldPos = chunkPos.getWorldPosition();

        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        float temperature;
        if (!SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            temperature = SnowUtils.getCachedBiomeTemperature(level, worldPos, currentSubSeason);
            if (temperature >= 0.5F) {
                clearSnowAndIce(level, chunkPos);
            } else if (temperature >= 0.15F) {
                accelerateMelt(level, chunkPos);
                meltingChunks.add(chunkPos);
            }
        } else {
            temperature = ForecastOrchestrator.getCurrentTemperature(new BiomeInstanceKey(level.getBiome(worldPos).unwrapKey().get().location(), worldPos), level.getDayTime());
            if (temperature >= 10.0F) {
                clearSnowAndIce(level, chunkPos);
            } else if (temperature >= 0.5F) {
                accelerateMelt(level, chunkPos);
                meltingChunks.add(chunkPos);
            }
        }



    }

    /**
     * Caches current block positions for the given players to avoid repeated
     * calls into player state from async workers.
     *
     * @param players online players to snapshot positions for
     */
    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }

    }

    /**
     * For each tracked player, computes a search radius from the simulation
     * distance and, if the temperature is warm enough, removes a number of
     * nearby snow blocks.
     *
     * @param level server level to modify
     */
    private static void replaceSnowBlocks(ServerLevel level) {
        Iterator var1 = playerPositions.entrySet().iterator();

        while (true) {
            BlockPos playerPos;
            int radius;
            int blocksToReplace;
            while (true) {
                if (!var1.hasNext()) {
                    return;
                }

                Map.Entry<ServerPlayer, BlockPos> entry = (Map.Entry) var1.next();
                ServerPlayer player = entry.getKey();
                playerPos = entry.getValue();
                int simulationDistance = getSimulationDistance(player);
                radius = Mth.clamp(simulationDistance * 16,16,64);
                if (!SereneSeasonsPlus.isProjectAtmosphereLoaded) {
                    Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                    float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);
                    if (!(temperature < 0.15F)) {
                        blocksToReplace = calculateBlocksToReplace(temperature);
                        break;
                    }
                } else {
                    float temperature = ForecastOrchestrator.getCurrentTemperature(new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos), level.getDayTime());
                    if (!((double) temperature < (double) 0.5F)) {
                        blocksToReplace = calculateBlocksToReplace1(temperature);
                        break;
                    }
                }
            }

            for (int i = 0; i < blocksToReplace; ++i) {
                BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                if (targetPos == null) {
                    break;
                }

                SnowUtils.breakOrDecrementLayer(level,targetPos);
            }
        }
    }

    /**
     * Computes how many snow blocks to remove based on temperature from
     * Project Atmosphere scale.
     *
     * @param temperature current temperature
     * @return number of blocks to attempt removing
     */
    private static int calculateBlocksToReplace1(float temperature) {
        return (int) Math.ceil((double) (temperature / 5.0F));
    }


    /**
     * Computes how many snow blocks to remove based on vanilla biome temperature.
     *
     * @param temperature biome base temperature
     * @return number of blocks to attempt removing
     */
    private static int calculateBlocksToReplace(float temperature) {
        if (temperature < 0.2F) {
            return 1;
        } else {
            return temperature < 0.5F ? 3 : 5;
        }
    }

    /**
     * Returns the server's simulation (view) distance in chunks converted to
     * blocks, with a sensible default if unavailable.
     *
     * @param player player for server access
     * @return view distance (in chunks) as blocks (approximate radius)
     */
    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }

    /**
     * Finds the first snow block within a cubic search around the center.
     * Returns null if none exists in the scanned range.
     *
     * @param level  level to scan
     * @param center center of the search
     * @param radius horizontal radius to search
     * @return the position of a snow block or null if none found
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

    /**
     * Processes queued melting chunks by randomly removing snow layers over time.
     * Chunks are removed from the queue once no snow remains.
     *
     * @param level server level to modify
     */
    private static void processMeltingChunks(Level level) {
        Iterator<ChunkPos> iterator = meltingChunks.iterator();
        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            boolean found = false;
            for (int i = 0; i < 5; ++i) {
                BlockPos snowPos = findSnowBlockInChunk(level, chunk);
                if (snowPos == null) {
                    break;
                }
                found = true;
                SnowUtils.breakOrDecrementLayer(level, snowPos);
            }
            if (!found) {
                iterator.remove();
            }
        }
    }

    /**
     * Finds a random snow block within the given chunk.
     *
     * @param level level to scan
     * @param chunk chunk to search
     * @return position of a snow block or null if none found
     */
    private static BlockPos findSnowBlockInChunk(Level level, LevelChunk chunk) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        ChunkPos cp = chunk.getPos();
        for (int attempts = 0; attempts < 64; ++attempts) {
            int x = cp.getMinBlockX() + RANDOM.nextInt(16);
            int z = cp.getMinBlockZ() + RANDOM.nextInt(16);
            int y = RANDOM.nextInt(maxY - minY) + minY;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Immediately removes snow and ice in the given chunk.
     *
     * @param level level to modify
     * @param chunkPos target chunk position
     */
    private static void clearSnowAndIce(Level level, ChunkPos chunkPos) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = minY; y < maxY; ++y) {
                    BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + x, y, chunkPos.getMinBlockZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    } else if (state.is(Blocks.ICE)) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /**
     * Reduces snow layers in a chunk to simulate a rapid warm-up.
     *
     * @param level level to modify
     * @param chunkPos target chunk position
     */
    private static void accelerateMelt(Level level, ChunkPos chunkPos) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = minY; y < maxY; ++y) {
                    BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + x, y, chunkPos.getMinBlockZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.SNOW_BLOCK)) {
                        level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4), 2);
                    } else if (state.is(Blocks.SNOW)) {
                        int layers = state.getValue(BlockStateProperties.LAYERS);
                        int newLayers = Math.max(0, layers - 3);
                        if (newLayers > 0) {
                            level.setBlock(pos, state.setValue(BlockStateProperties.LAYERS, newLayers), 2);
                        } else {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    } else if (state.is(Blocks.ICE)) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
    }


}






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
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

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
import net.minecraft.world.level.levelgen.Heightmap;
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
    private static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();
    private static final Set<ChunkPos> meltingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<ChunkPos> chunksToClear = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

                // Always drain a small budget of chunks queued from load to avoid load-time stalls
                if (!chunksToClear.isEmpty()) {
                    processChunksToClear(level, 2); // process up to 2 chunks per tick
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
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: non-LevelChunk, skipping");
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: client side, skipping");
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        BlockPos worldPos = chunkPos.getWorldPosition();

        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        float temperature;
        if (!SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            temperature = SnowUtils.getCachedBiomeTemperature(level, worldPos, currentSubSeason);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: {} temp={} (SS scale)", chunkPos, temperature);
            if (temperature >= 0.5F) {
                chunksToClear.add(chunkPos);
                if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: queued clear {} (queue={})", chunkPos, chunksToClear.size());
            } else if (temperature >= 0.15F) {
                long t0 = System.nanoTime();
                accelerateMelt(level, chunkPos);
                meltingChunks.add(chunkPos);
                if (LOGGER.isDebugEnabled()) {
                    long dt = (System.nanoTime() - t0) / 1_000_000L;
                    LOGGER.debug("onChunkLoad: accelerated melt {} in {} ms; queued gradual melt (size={})", chunkPos, dt, meltingChunks.size());
                }
            }
        } else {
            temperature = ForecastOrchestrator.getCurrentTemperature(new BiomeInstanceKey(level.getBiome(worldPos).unwrapKey().get().location(), worldPos), level.getDayTime());
            if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: {} temp={} (PA scale)", chunkPos, temperature);
            if (temperature >= 10.0F) {
                chunksToClear.add(chunkPos);
                if (LOGGER.isDebugEnabled()) LOGGER.debug("onChunkLoad: queued clear {} (queue={})", chunkPos, chunksToClear.size());
            } else if (temperature >= 0.5F) {
                long t0 = System.nanoTime();
                accelerateMelt(level, chunkPos);
                meltingChunks.add(chunkPos);
                if (LOGGER.isDebugEnabled()) {
                    long dt = (System.nanoTime() - t0) / 1_000_000L;
                    LOGGER.debug("onChunkLoad: accelerated melt {} in {} ms; queued gradual melt (size={})", chunkPos, dt, meltingChunks.size());
                }
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
        long t0All = System.nanoTime();
        Iterator<ChunkPos> iterator = meltingChunks.iterator();
        int processed = 0;
        Set<ChunkPos> toRemove = new HashSet<>();
        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            boolean found = false;
            long t0 = System.nanoTime();
            for (int i = 0; i < 5; ++i) {
                BlockPos snowPos = findSnowBlockInChunk(level, chunk);
                if (snowPos == null) {
                    break;
                }
                found = true;
                SnowUtils.breakOrDecrementLayer(level, snowPos);
            }
            if (!found) {
                toRemove.add(pos);
            }
            if (LOGGER.isDebugEnabled()) {
                long dt = (System.nanoTime() - t0) / 1_000_000L;
                LOGGER.debug("processMeltingChunks: chunk {} took {} ms{}", pos, dt, found ? "" : " (no snow, removed from queue)");
            }
            processed++;
        }
        if (!toRemove.isEmpty()) {
            meltingChunks.removeAll(toRemove);
        }
        if (LOGGER.isDebugEnabled()) {
            long dtAll = (System.nanoTime() - t0All) / 1_000_000L;
            LOGGER.debug("processMeltingChunks: processed {} entries in {} ms (remaining={})", processed, dtAll, meltingChunks.size());
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
        long t0 = System.nanoTime();
        int minY = level.getMinBuildHeight();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; ++dx) {
            int worldX = baseX + dx;
            for (int dz = 0; dz < 16; ++dz) {
                int worldZ = baseZ + dz;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
                int startY = topY + 1; // include snow that sits above the surface
                int endY = Math.max(minY, topY - 8); // scan a small band below the surface

                // Quick skip if this column doesn't present snow/ice at surface
                BlockState surface = level.getBlockState(mutable.set(worldX, topY, worldZ));
                BlockState above = level.getBlockState(mutable.set(worldX, topY + 1, worldZ));
                if (!(surface.is(Blocks.SNOW) || surface.is(Blocks.SNOW_BLOCK) || surface.is(Blocks.ICE)
                        || above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK))) {
                    continue;
                }

                for (int y = startY; y >= endY; --y) {
                    mutable.set(worldX, y, worldZ);
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
                    } else if (state.is(Blocks.ICE)) {
                        level.setBlock(mutable, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            long dt = (System.nanoTime() - t0) / 1_000_000L;
            LOGGER.debug("clearSnowAndIce: {} took {} ms", chunkPos, dt);
        }
    }

    /**
     * Reduces snow layers in a chunk to simulate a rapid warm-up.
     *
     * @param level level to modify
     * @param chunkPos target chunk position
     */
    private static void accelerateMelt(Level level, ChunkPos chunkPos) {
        long t0 = System.nanoTime();
        int minY = level.getMinBuildHeight();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; ++dx) {
            int worldX = baseX + dx;
            for (int dz = 0; dz < 16; ++dz) {
                int worldZ = baseZ + dz;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
                int startY = topY + 1;
                int endY = Math.max(minY, topY - 8);

                // Quick skip if this column doesn't present snow/ice at surface
                BlockState surface = level.getBlockState(mutable.set(worldX, topY, worldZ));
                BlockState above = level.getBlockState(mutable.set(worldX, topY + 1, worldZ));
                if (!(surface.is(Blocks.SNOW) || surface.is(Blocks.SNOW_BLOCK) || surface.is(Blocks.ICE)
                        || above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK))) {
                    continue;
                }

                for (int y = startY; y >= endY; --y) {
                    mutable.set(worldX, y, worldZ);
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(Blocks.SNOW_BLOCK)) {
                        level.setBlock(mutable, Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4), 2);
                    } else if (state.is(Blocks.SNOW)) {
                        int layers = state.getValue(BlockStateProperties.LAYERS);
                        int newLayers = Math.max(0, layers - 3);
                        if (newLayers > 0) {
                            level.setBlock(mutable, state.setValue(BlockStateProperties.LAYERS, newLayers), 2);
                        } else {
                            level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
                        }
                    } else if (state.is(Blocks.ICE)) {
                        level.setBlock(mutable, Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            long dt = (System.nanoTime() - t0) / 1_000_000L;
            LOGGER.debug("accelerateMelt: {} took {} ms", chunkPos, dt);
        }
    }

    /**
     * Processes a limited number of queued chunks that require immediate clearing.
     * Avoids running heavy logic in the ChunkEvent.Load handler.
     */
    private static void processChunksToClear(Level level, int budget) {
        if (budget <= 0) return;
        if (LOGGER.isDebugEnabled()) LOGGER.debug("processChunksToClear: start budget={} queue={}", budget, chunksToClear.size());
        Iterator<ChunkPos> it = chunksToClear.iterator();
        while (budget > 0 && it.hasNext()) {
            ChunkPos pos = it.next();
            BlockPos check = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
            if (!level.hasChunkAt(check)) {
                // Skip for now; keep it queued until the chunk is loaded again
                continue;
            }
            long t0 = System.nanoTime();
            clearSnowAndIce(level, pos);
            chunksToClear.remove(pos);
            budget--;
            if (LOGGER.isDebugEnabled()) {
                long dt = (System.nanoTime() - t0) / 1_000_000L;
                LOGGER.debug("processChunksToClear: cleared {} in {} ms (budget left={} queue={})", pos, dt, budget, chunksToClear.size());
            }
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug("processChunksToClear: end (remaining queue={})", chunksToClear.size());
    }


}

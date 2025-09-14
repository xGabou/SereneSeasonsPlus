package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommonSnowBlockReplacer {
    public static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    protected static final Random RANDOM = new Random();
    protected static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();
    protected static final Set<ChunkPos> meltingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected static final Set<ChunkPos> chunksToClear = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected static int tickThresholdSnowReplacer;
    protected static int tickThresholdSnowReplacerForHotSeasons = 30;
    protected static int tickCounter = 0;
    public static SnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();

    private static final Queue<ChunkPos> meltQueue = new ArrayDeque<>();
    private static final int MAX_MELT_CHUNKS_PER_TICK = 8; // tune this



    public static void handleServerTick(MinecraftServer server) {
        ++tickCounter;
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
                processChunksToClear(level,30 ); // process up to 2 chunks per tick
            }
            if (!meltQueue.isEmpty()) {
                int processed = 0;
                while (processed < MAX_MELT_CHUNKS_PER_TICK && !meltQueue.isEmpty()) {
                    ChunkPos pos = meltQueue.poll();
                    if (pos == null) break;

                    processChunks(level, pos.getWorldPosition(),
                            SeasonHelper.getSeasonState(level).getSubSeason(), pos);
                    processed++;
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
    protected static void updatePlayerPositions(Iterable<ServerPlayer> players) {
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
    protected static void replaceSnowBlocks(ServerLevel level) {
        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            int simulationDistance = getSimulationDistance(player);
            int radius = Mth.clamp(simulationDistance * 16, 16, 64);

            int blocksToReplace = HANDLER.getBlocksToReplace(level, playerPos);

            for (int i = 0; i < blocksToReplace; ++i) {
                BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                if (targetPos == null) break;

                SnowUtils.breakOrDecrementLayer(level, targetPos);
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
    protected static int calculateBlocksToReplace1(float temperature) {
        return (int) Math.ceil((double) (temperature / 5.0F));
    }


    /**
     * Computes how many snow blocks to remove based on vanilla biome temperature.
     *
     * @param temperature biome base temperature
     * @return number of blocks to attempt removing
     */
    protected static int calculateBlocksToReplace(float temperature) {
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
    protected static int getSimulationDistance(ServerPlayer player) {
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
    protected static BlockPos findSnowBlockInRadius(ServerLevel level, BlockPos center, int radius) {
        ChunkSource chunkSource = level.getChunkSource();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                for (int y = -5; y <= 5; ++y) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    ChunkAccess chunk = chunkSource.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                    if (chunk == null) continue;

                    if (chunk.getBlockState(pos).is(Blocks.SNOW_BLOCK)) {
                        return pos.immutable();
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
    protected static void processMeltingChunks(Level level) {
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
    protected static BlockPos findSnowBlockInChunk(Level level, LevelChunk chunk) {
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
     * @param level    level to modify
     * @param chunkPos target chunk position
     */
    protected static void clearSnowAndIce(Level level, ChunkPos chunkPos) {
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
                        level.getChunk(mutable).setBlockState(mutable, Blocks.AIR.defaultBlockState(), false);
                    } else if (state.is(Blocks.ICE)) {
                        level.getChunk(mutable).setBlockState(mutable, Blocks.WATER.defaultBlockState(), false);
                    }
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            long dt = (System.nanoTime() - t0) / 1_000_000L;
            LOGGER.debug("clearSnowAndIce: {} took {} ms", chunkPos, dt);
        }
    }

    public static void handleOnChunkLoad(LevelChunk chunk) {
        meltQueue.add(chunk.getPos());
    }



    protected static void processChunks(Level level, BlockPos worldPos, Season.SubSeason currentSubSeason, ChunkPos chunkPos) {
        HANDLER.processChunks(level, worldPos, currentSubSeason, chunkPos);
    }

    /**
     * Reduces snow layers in a chunk to simulate a rapid warm-up.
     *
     * @param level    level to modify
     * @param chunkPos target chunk position
     */
    protected static void accelerateMelt(Level level, ChunkPos chunkPos) {
        long t0 = System.nanoTime();
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) return; // skip if not loaded


        Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);

        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int topY = heightmap.getFirstAvailable(dx, dz);
                int worldX = chunkPos.getMinBlockX() + dx;
                int worldZ = chunkPos.getMinBlockZ() + dz;

                BlockState surface = chunk.getBlockState(mutable.set(worldX, topY, worldZ));
                BlockState above   = chunk.getBlockState(mutable.set(worldX, topY + 1, worldZ));

                if (!(surface.is(Blocks.SNOW) || surface.is(Blocks.SNOW_BLOCK) || surface.is(Blocks.ICE)
                        || above.is(Blocks.SNOW) || above.is(Blocks.SNOW_BLOCK))) {
                    continue;
                }

                int startY = topY + 1;
                int endY = Math.max(minY, topY - 8);

                for (int y = startY; y >= endY; --y) {
                    mutable.set(worldX, y, worldZ);
                    BlockState state = chunk.getBlockState(mutable);

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
    protected static void processChunksToClear(Level level, int budget) {
        if (budget <= 0) return;
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("processChunksToClear: start budget={} queue={}", budget, chunksToClear.size());
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
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("processChunksToClear: end (remaining queue={})", chunksToClear.size());
    }

    public static void onServerStarting(int config) {
        tickThresholdSnowReplacer = config;
        tickCounter = 0;
        playerPositions.clear();
        LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
        meltQueue.clear();
        meltingChunks.clear();
        chunksToClear.clear();

    }

    /**
     * Refreshes the tick threshold and async config on server tick,
     * allowing live config changes.
     */
    public static void onConfigReload(int config) {
        tickThresholdSnowReplacer = config;
    }
}

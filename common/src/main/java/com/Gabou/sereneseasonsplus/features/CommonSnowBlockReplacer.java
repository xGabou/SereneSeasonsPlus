package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.storage.Memory;
import com.Gabou.sereneseasonsplus.storage.MemoryHandler;
import com.Gabou.sereneseasonsplus.tags.SSPTags;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
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
import sereneseasons.init.ModTags;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CommonSnowBlockReplacer {
    public static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    protected static final Random RANDOM = new Random();
    protected static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();
    protected static final Set<ChunkPos> meltingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected static int tickThresholdSnowReplacer;
    protected static int tickThresholdSnowReplacerForHotSeasons = 30;
    protected static int tickCounter = 0;
    public static SnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();

    private static final Queue<ChunkPos> meltQueue = new ArrayDeque<>();
    private static final int MAX_MELT_CHUNKS_PER_TICK = 32; // higher throughput for initial classification


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

            if (ChunkQueue.isEmpty()) {
                ChunkQueue.shuffle();
                return;
            }

            for (int i = 0; i < Mth.clamp(ChunkQueue.size(), 0, MemoryHandler.getMaxChunksToProcessPerTick()); i++) {
                ChunkQueue.Entry entry = ChunkQueue.pop();
                ChunkPos chunkPos = entry.pos();
                int sittingFor = entry.sittingFor();

                // Check that not only the chunk is loaded, but its neighbors too. This is because, when sending block
                // updates that cross a chunk boundary, it will block until that chunk is loaded, which will cause
                // immense delay. The advantage of doing it all in a massive if-statement is that we immediately break if
                // one of the chunks isn't loaded, instead of waiting to check them all.
                if (!level.hasChunk(chunkPos.x, chunkPos.z)
                        || !level.hasChunk(chunkPos.x - 1, chunkPos.z - 1)
                        || !level.hasChunk(chunkPos.x + 1, chunkPos.z - 1)
                        || !level.hasChunk(chunkPos.x - 1, chunkPos.z + 1)
                        || !level.hasChunk(chunkPos.x + 1, chunkPos.z + 1)) {
                    if (sittingFor < 100) {
                        ChunkQueue.add(new ChunkQueue.Entry(chunkPos, sittingFor + 1), false);
                        i--; // Act as if we never began processing this chunk, to make sure we at least process something.
                    } else {
                        // It's been sitting for a while, and it's still not loaded. Let's just forget it -- it'll make its
                        // way back into the queue eventually.
                        Memory.forget(chunkPos);
                    }
                    continue;
                }
                processChunksToClear(level, budget);
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
        protected static void updatePlayerPositions (Iterable < ServerPlayer > players) {
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
        protected static void replaceSnowBlocks (ServerLevel level){
            for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
                ServerPlayer player = entry.getKey();
                BlockPos playerPos = entry.getValue();

                int simulationDistance = getSimulationDistance(player);
                int radius = Mth.clamp(simulationDistance * 16, 16, 64);

                int blocksToReplace = HANDLER.getBlocksToReplace(level, playerPos);

                for (int i = 0; i < blocksToReplace; ++i) {
                    BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                    if (targetPos == null) continue;

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
        protected static int calculateBlocksToReplace1 ( float temperature){
            return (int) Math.ceil((double) (temperature / 5.0F));
        }


        /**
         * Computes how many snow blocks to remove based on vanilla biome temperature.
         *
         * @param temperature biome base temperature
         * @return number of blocks to attempt removing
         */
        protected static int calculateBlocksToReplace ( float temperature){
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
        protected static int getSimulationDistance (ServerPlayer player){
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
        protected static BlockPos findSnowBlockInRadius (ServerLevel level, BlockPos center,int radius){
            // Run synchronously on the main server thread
            return level.getServer().submit(() -> {
                ChunkSource chunkSource = level.getChunkSource();
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

                for (int x = -radius; x <= radius; ++x) {
                    for (int z = -radius; z <= radius; ++z) {
                        int chunkX = (center.getX() + x) >> 4;
                        int chunkZ = (center.getZ() + z) >> 4;

                        ChunkAccess chunk = chunkSource.getChunk(chunkX, chunkZ, false);
                        if (chunk == null) {
                            // Skip the entire column, no need to try Y loop
                            continue;
                        }

                        for (int y = -5; y <= 5; ++y) {
                            pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                            // Safety: skip invalid build heights
                            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                                continue;
                            }

                            if (chunk.getBlockState(pos).is(SSPTags.Blocks.MELTABLE)) {
                                return pos.immutable();
                            }
                        }
                    }
                }

                return null;
            }).join(); // block until the result is computed
        }


        /**
         * Processes queued melting chunks by randomly removing snow layers over time.
         * Chunks are removed from the queue once no snow remains.
         *
         * @param level server level to modify
         */
        protected static void processMeltingChunks (Level level){
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
        protected static BlockPos findSnowBlockInChunk (Level level, LevelChunk chunk){
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
        protected static void clearSnowAndIce (Level level, ChunkPos chunkPos){
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
                            level.setBlock(mutable, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                        } else if (state.is(Blocks.ICE)) {
                            level.setBlock(mutable, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                        }
                    }
                }
            }
            if (LOGGER.isDebugEnabled()) {
                long dt = (System.nanoTime() - t0) / 1_000_000L;
                LOGGER.debug("clearSnowAndIce: {} took {} ms", chunkPos, dt);
            }
        }

        public static void handleOnChunkLoad (LevelChunk chunk){
            meltQueue.add(chunk.getPos());
        }


        protected static void processChunks (ServerLevel level, BlockPos worldPos, Season.SubSeason
        currentSubSeason, ChunkPos chunkPos){
            HANDLER.processChunks(level, worldPos, currentSubSeason, chunkPos);
        }

        /**
         * Reduces snow layers in a chunk to simulate a rapid warm-up.
         *
         * @param level    the world level to modify
         * @param chunkPos the chunk position to process
         */
        protected static void accelerateMelt (ServerLevel level, ChunkPos chunkPos){
            long t0 = System.nanoTime();

            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight(); // optional safety check
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            ChunkAccess chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            if (!(chunk instanceof LevelChunk liveChunk)) return; // Ensure full chunk access


            Heightmap heightmap = liveChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);

            for (int dx = 0; dx < 16; ++dx) {
                int worldX = chunkPos.getMinBlockX() + dx;

                for (int dz = 0; dz < 16; ++dz) {
                    int worldZ = chunkPos.getMinBlockZ() + dz;

                    int topY = heightmap.getFirstAvailable(dx, dz);
                    int startY = topY + 1;
                    int endY = Math.max(minY, topY - 8);

                    // Reuse mutable for read
                    mutable.set(worldX, topY, worldZ);
                    BlockState surface = liveChunk.getBlockState(mutable);

                    mutable.setY(topY + 1);
                    BlockState above = (topY + 1 < maxY) ? liveChunk.getBlockState(mutable) : Blocks.AIR.defaultBlockState();

                    if (!surface.is(SSPTags.Blocks.MELTABLE) && !above.is(SSPTags.Blocks.MELTABLE)) {
                        continue;
                    }

                    for (int y = startY; y >= endY; --y) {
                        mutable.setY(y);
                        BlockState state = liveChunk.getBlockState(mutable);

                        if (state.is(Blocks.SNOW_BLOCK)) {
                            level.setBlock(
                                    mutable,
                                    Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4),
                                    Block.UPDATE_ALL
                            );

                        } else if (state.is(Blocks.SNOW)) {
                            int layers = state.getValue(BlockStateProperties.LAYERS);
                            int newLayers = layers - 3;

                            if (newLayers > 0) {
                                level.setBlock(
                                        mutable,
                                        state.setValue(BlockStateProperties.LAYERS, newLayers),
                                        Block.UPDATE_ALL
                                );
                            } else {
                                level.setBlock(mutable, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }

                        } else if (state.is(Blocks.ICE)) {
                            level.setBlock(mutable, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
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
        protected static void processChunksToClear (Level level,int budget){
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

        public static void onServerStarting ( int config){
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
        public static void onConfigReload ( int config){
            tickThresholdSnowReplacer = config;
        }

        /**
         * Called when the season changes. Proactively classifies nearby loaded chunks
         * around players into either full-clear queue or accelerated-melt queue based on
         * current biome temperature, so the world updates without requiring a reload.
         */
        public static void onSeasonChange (ServerLevel level){
            Season.SubSeason sub = SeasonHelper.getSeasonState(level).getSubSeason();
            var players = level.getServer().getPlayerList().getPlayers();
            if (players.isEmpty()) return;

            for (ServerPlayer player : players) {
                int view = getSimulationDistance(player);
                int cx = player.chunkPosition().x;
                int cz = player.chunkPosition().z;

                for (int dx = -view; dx <= view; ++dx) {
                    for (int dz = -view; dz <= view; ++dz) {
                        ChunkAccess acc = level.getChunkSource().getChunk(cx + dx, cz + dz, false);
                        if (!(acc instanceof LevelChunk chunk)) continue;
                        ChunkPos cp = chunk.getPos();
                        DefaultSnowEnvironmentHandler.doMeltingLogic(level, sub, cp, SnowUtils.getCachedBiomeTemperature(level, cp.getWorldPosition(), sub));
                    }
                }
            }
        }


    }

    package com.Gabou.sereneseasonsplus.features;

    import com.Gabou.sereneseasonsplus.features.snowstorm.ISnowStormLevel;
    import com.Gabou.sereneseasonsplus.mixin.ChunkMapMixin;
    import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
    import com.Gabou.sereneseasonsplus.storage.Memory;
    import com.Gabou.sereneseasonsplus.storage.MemoryHandler;
    import com.Gabou.sereneseasonsplus.storage.Priority;
    import com.Gabou.sereneseasonsplus.tags.SSPTags;
    import com.Gabou.sereneseasonsplus.util.*;
    import net.minecraft.core.BlockPos;
    import net.minecraft.server.MinecraftServer;
    import net.minecraft.server.level.ChunkHolder;
    import net.minecraft.server.level.ServerLevel;
    import net.minecraft.server.level.ServerPlayer;
    import net.minecraft.util.Mth;
    import net.minecraft.util.RandomSource;
    import net.minecraft.world.level.ChunkPos;
    import net.minecraft.world.level.Level;
    import net.minecraft.world.level.block.Blocks;
    import net.minecraft.world.level.block.Block;
    import net.minecraft.world.level.block.SnowLayerBlock;
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
    import java.util.function.BiConsumer;

    public class CommonSnowBlockFeature {
        public static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
        protected static final Random RANDOM = new Random();
        protected static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();
        public static final int MAX_MEMORY = 10000;
        private static final String INITIAL_SNOW_PERSISTENCE_KEY = "sereneseasonsplus.initial_snow";
        private static final int INITIAL_SNOW_SCAN_DEPTH = 8;

        protected static boolean snowStormEnabled = false;
        protected static int tickThresholdSnowReplacer;
        protected static int tickThresholdSnowReplacerForHotSeasons = 30;
        protected static int tickCounter = 0;
        public static SnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();


        protected static int snowStormIntensity = 0;

        protected static boolean isSnowStorming = false;

        /**
         * Sampling attempts per call. Tune as desired.
         */
        protected static final int MAX_ATTEMPTS = 12;

        public static int getTickCounter() {
            return tickCounter;
        }


        public static final class NeedRefilling {
            private static boolean value;
            private static int counter;

            public static int getCounter() {
                return counter;
            }

            public static void set(boolean newValue, int newCounter) {
                value = newValue;
                counter = newCounter;
            }

            public static boolean doesNeedRefilling() {
                return value && counter > 5 && coolDown > 200;
            }
        }


        protected static int coolDown = 0;

        public static void handleServerTick(MinecraftServer server, ServerLevel level) {
            long t0All = System.nanoTime();
            ++tickCounter;

            if (level != null && !level.isClientSide()) {
                int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 16);
                if (tickCounter % tickThresholdSnowReplacer == 0 && tickCounter > 500) {
                    updatePlayerPositions(level.players());
                    passifSnowBlocks(level);

                    if (NeedRefilling.doesNeedRefilling() && ChunkQueue.areEmpty()) {
                        doLogicForRefilling(level);

                    }

                }
                if (ChunkQueue.isEmpty()) {
                    ChunkQueue.shuffle();
                    long dtAll = (System.nanoTime() - t0All) / 1_000_000L;

                    NeedRefilling.set(true, NeedRefilling.getCounter() + 1);

                    if (NeedRefilling.getCounter() > 5)
                        ++coolDown;
                    //LOGGER.info("Processed up to {} chunks in {} ms; queue size={}; CoolDown:{} ", MemoryHandler.getMaxChunksToProcessPerTick(), dtAll, ChunkQueue.size(), coolDown);
                    return;
                }
                if (Memory.isFull() || ChunkQueue.isFull()) {
                    ChunkQueue.clear();
                    doLogicForRefilling(level);
                    return;
                }
                NeedRefilling.set(false, 0);
                //LOGGER.info("Will try to process {} chunks", ChunkQueue.size());
                int oldSize = ChunkQueue.size();
                for (int i = 0; i < Mth.clamp(ChunkQueue.size(), 0, MemoryHandler.getMaxChunksToProcessPerTick()); i++) {
                    long t0 = System.nanoTime();
                    ChunkQueue.Entry entry = ChunkQueue.pop();
                    ChunkPos chunkPos = entry.pos();
                    if (chunkPos == null) break;
                    int sittingFor = entry.sittingFor();
                    int workLeft = entry.workLeft();

                    long lastSkipped = entry.lastSkipped();

                    // Check that not only the chunk is loaded, but its neighbors too. This is because, when sending block
                    // updates that cross a chunk boundary, it will block until that chunk is loaded, which will cause
                    // immense delay. The advantage of doing it all in a massive if-statement is that we immediately break if
                    // one of the chunks isn't loaded, instead of waiting to check them all.
                    long now = level.getGameTime();
                    if (!level.hasChunk(chunkPos.x, chunkPos.z)
                            || !level.hasChunk(chunkPos.x - 1, chunkPos.z - 1)
                            || !level.hasChunk(chunkPos.x + 1, chunkPos.z - 1)
                            || !level.hasChunk(chunkPos.x - 1, chunkPos.z + 1)
                            || !level.hasChunk(chunkPos.x + 1, chunkPos.z + 1)) {


                        if (sittingFor < 100) {
                            ChunkQueue.add(new ChunkQueue.Entry(chunkPos, sittingFor + 1, workLeft, now), false);
                            i--; // Act as if we never began processing this chunk, to make sure we at least process something.
                        } else {
                            // It's been sitting for a while, and it's still not loaded. Let's just forget it -- it'll make its
                            // way back into the queue eventually.
                            Memory.remember(chunkPos);
                        }
                        continue;
                    }
                    if (now - lastSkipped < MemoryHandler.getForgetTime()) {
                        i--;
                        continue;
                    }

                    processChunk(level,
                            SeasonHelper.getSeasonState(level).getSubSeason(), SnowUtils.getCachedBiomeTemperature(level, chunkPos.getMiddleBlockPosition(65), SeasonHelper.getSeasonState(level).getSubSeason()), entry);
                    if (!((MinecraftServerAccess)server).sereneseasonsplus$tempsEcoule() && i >= MemoryHandler.getMinChunksToProcessPerTick()) {
                        break;

                    }

                    long dt = (System.nanoTime() - t0) / 1_000_000L;
                    //LOGGER.info("Processed chunk {} in {} ms; queue size={}", chunkPos, dt, ChunkQueue.size());
                }


                long dtAll = (System.nanoTime() - t0All) / 1_000_000L;
                //LOGGER.info("Processed up to {} chunks in {} ms; queue size={}; old queue size ={}", MemoryHandler.getMaxChunksToProcessPerTick(), dtAll, ChunkQueue.size(), oldSize);

            }
        }

        private static void doLogicForRefilling(ServerLevel level) {
            NeedRefilling.set(false, 0);
            Memory.erase();
            ChunkMapInterfaceAccess chunkMap = (ChunkMapInterfaceAccess) level.getChunkSource().chunkMap;

            int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 8);
            for (ChunkHolder chunk : chunkMap.snow$getChunksSafe() ) {
                ChunkPos chunkPos = chunk.getPos();

                boolean nearPlayer = playerPositions.values().stream().anyMatch(playerPos -> {
                    ChunkPos playerChunk = new ChunkPos(playerPos);
                    int dx = Math.abs(playerChunk.x - chunkPos.x);
                    int dz = Math.abs(playerChunk.z - chunkPos.z);
                    return dx <= viewDist && dz <= viewDist;
                });

                if (nearPlayer) {
                    ChunkQueue.tryAdd(chunkPos, false);
                }
            }

            coolDown = 0;
        }




        public static void processChunk(ServerLevel level, Season.SubSeason sub, float temperature, ChunkQueue.Entry entry) {
            int workLeft = entry.workLeft();
            ChunkPos chunkPos = entry.pos();
            //        if(entry.pos().equals(new ChunkPos(341, -97)))
            //            //LOGGER.info("Processing chunk 341,-97");
            LevelChunk levelChunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            if (levelChunk != null) {
                ISnowTrackedChunk trackedChunk = (ISnowTrackedChunk) levelChunk;
                if (trackedChunk.sereneseasonsplus$shouldApplyInitialSnow()
                        && !trackedChunk.sereneseasonsplus$hasAppliedInitialSnow()) {
                    LayerBounds deepWinterBounds = getDeepWinterLayerBounds(sub, SeasonHelper.getSeasonState(level).getDay());
                    if (deepWinterBounds != null) {
                        boolean placed = applyDeepWinterInitialSnow(level, chunkPos, deepWinterBounds);
                        if (placed) {
                            markChunkWithInitialSnow(levelChunk);
                            trackedChunk.sereneseasonsplus$setHasAppliedInitialSnow(true);
                        }
                        trackedChunk.sereneseasonsplus$setShouldApplyInitialSnow(false);
                        Memory.remember(chunkPos);
                        return;
                    }
                    trackedChunk.sereneseasonsplus$setShouldApplyInitialSnow(false);
                }
            }
            BlockPos pos = chunkPos.getMiddleBlockPosition(65);
            WeatherDecision decision = HANDLER.decideWeatherAction(level, sub, temperature, level.getBiome(pos).value().coldEnoughToSnow(pos), pos);
            int budget = switch (decision.priority()) {
                case URGENT -> 256;       // full wipe
                case ACCELERATED -> level.getRandom().nextInt(48) + 1;   // faster
                case GRADUAL -> level.getRandom().nextInt(5) + 1; // 1–5
            };
            switch (decision.action()) {
                case SNOW -> doSnowFall(level, entry, budget);

                case MELT -> {
                    if (decision.priority() == Priority.ACCELERATED) {
                        accelerateMelt(level, chunkPos, budget);
                    } else {
                        processMeltingChunk(level, chunkPos); // gradual melt already random-based
                    }
                }

                case CLEAR -> clearSnowAndIce(level, chunkPos); // full wipe, no budget

                case NONE -> {
                    Memory.remember(chunkPos);
                    return;
                } // skip
            }

            // requeue if still work left
            if (workLeft > budget && decision.priority() == Priority.URGENT) {
                ChunkQueue.add(entry, false);
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
        protected static void passifSnowBlocks(ServerLevel level) {
            for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
                ServerPlayer player = entry.getKey();
                BlockPos playerPos = entry.getValue();

                int simulationDistance = getSimulationDistance(player);
                int radius = Mth.clamp(simulationDistance * 16, 16, 64);

                int blocksToReplace = HANDLER.getBlocksToReplace(level, playerPos);
                final RandomSource random = level.random;
                final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
                if(blocksToReplace < 0) {
                    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                        final int dx = random.nextInt(radius * 2 + 1) - radius;
                        final int dz = random.nextInt(radius * 2 + 1) - radius;
                        final int x = playerPos.getX() + dx;
                        final int z = playerPos.getZ() + dz;
                        final int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                        pos.set(x, y, z);
                        below.set(x, y - 1, z);


                        final BlockState stateAt = level.getBlockState(pos);
                        final boolean canStackSnow =
                                stateAt.is(Blocks.SNOW)
                                        && stateAt.hasProperty(SnowLayerBlock.LAYERS)
                                        && stateAt.getValue(SnowLayerBlock.LAYERS) < 8;

                        if (!(level.isEmptyBlock(pos) || canStackSnow)) continue;


                        if (level.getBlockState(below).is(Blocks.WATER)) continue;

                        placeSingleLayerNoClimb(level, pos);
                    }
                }
                else{
                    for (int i = 0; i < blocksToReplace; ++i) {
                        BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                        if (targetPos == null) continue;
                        SnowUtils.breakOrDecrementLayer(level, targetPos);
                    }
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
            // Run synchronously on the main server thread

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
        }


        private static void iterateChunkColumns(ServerLevel level, ChunkPos chunkPos, BiConsumer<BlockPos.MutableBlockPos, BlockState> action) {
            int minY = level.getMinBuildHeight();
            int baseX = chunkPos.getMinBlockX();
            int maxX = chunkPos.getMaxBlockX();
            int baseZ = chunkPos.getMinBlockZ();
            int maxZ = chunkPos.getMaxBlockZ();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            for (int worldX = baseX; worldX <= maxX; ++worldX) {
                for (int worldZ = baseZ; worldZ <= maxZ; ++worldZ) {
                    columnsLogic(level, action, worldX, worldZ, minY, mutable);
                }
            }
        }

        private static void columnsLogic(ServerLevel level, BiConsumer<BlockPos.MutableBlockPos, BlockState> action, int worldX, int worldZ, int minY, BlockPos.MutableBlockPos mutable) {
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
            checkHeight(level, action, worldX, worldZ, minY, mutable, topY);

            // Handle snow under trees as well
            BlockPos noLeavesPos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(worldX, 0, worldZ)
            );
            if (noLeavesPos.getY() != topY) {
                int nlTopY = noLeavesPos.getY();
                checkHeight(level, action, worldX, worldZ, minY, mutable, nlTopY);
            }
        }

        private static void checkHeight(ServerLevel level, BiConsumer<BlockPos.MutableBlockPos, BlockState> action, int worldX, int worldZ, int minY, BlockPos.MutableBlockPos mutable, int nlTopY) {
            int nlStart = nlTopY + 1;
            int nlEnd = Math.max(minY, nlTopY - 8);

            for (int y = nlStart; y >= nlEnd; --y) {
                mutable.set(worldX, y, worldZ);
                BlockState state = level.getBlockState(mutable);
                action.accept(mutable, state);
            }
        }

        public static void doSnowFall(ServerLevel level, ChunkQueue.Entry entry, int budget) {
            ChunkPos chunkPos = entry.pos();




            // Determine storm state (optional integration); do not gate by temperature here
            boolean stormActive = CommonSnowBlockFeature.isSnowStormAt(level, chunkPos);
            int stormIntensity = CommonSnowBlockFeature.getSnowStormIntensity(level, chunkPos);

            // Access per-chunk tracked state
            var access = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            ISnowTrackedChunk tracked = null;
            if (access instanceof LevelChunk) {
                tracked = (ISnowTrackedChunk) access;
                tracked.sereneseasonsplus$incrementSnowCount();

            }



            // Nearby players?
            boolean nearPlayers = isChunkNearAnyPlayer(level, chunkPos);

            // doSnowFall is only called when handler decided SNOW action; avoid extra gating here

            // Player-based piling for natural randomness when players are nearby
            if (nearPlayers) {
                if (stormActive) {
                    // Player-distributed storm piling using intensity cap
                    doPlayerBasedPiling(level, chunkPos, Math.max(1, Math.min(8, stormIntensity)), budget);
                } else {
                    // Light snowfall around players (single-layer increments)
                    doPlayerBasedPiling(level, chunkPos, 2, Math.max(1, budget));
                }
                // keep gentle ongoing piling while snow continues
                if (level.getRandom().nextInt(3) != 0) {
                    ChunkQueue.add(entry, false);
                }
                return;
            }

            // No players nearby: add a noisy random 1-4 layers once per storm
            if (tracked != null && !tracked.sereneseasonsplus$hasReceivedSnowLayerThisStorm()) {
                applySingleNoisyLayer(level, chunkPos);
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);
            }
        }

        private static void sampleRandomChunkColumns(ServerLevel level, ChunkPos chunkPos, int attempts, BiConsumer<BlockPos.MutableBlockPos, BlockState> action) {
            int minY = level.getMinBuildHeight();
            int baseX = chunkPos.getMinBlockX();
            int baseZ = chunkPos.getMinBlockZ();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            RandomSource random = level.random;
            for (int i = 0; i < attempts; i++) {
                int worldX = baseX + random.nextInt(16);
                int worldZ = baseZ + random.nextInt(16);

                // get top surface height
                columnsLogic(level, action, worldX, worldZ, minY, mutable);
            }
        }


        protected static void doSnowStormPiling(ServerLevel level, ChunkPos chunkPos,
                                                int maxLayers, int budget) {
            final int[] processed = {0};
            final RandomSource rng = level.random; // reuse

            int cap = Mth.clamp(maxLayers, 1, 8);

            sampleRandomChunkColumns(level, chunkPos, 16, (pos, state) -> {
                if (processed[0] >= budget) return;

                int step = (cap <= 2) ? 1 : rng.nextInt(1, cap); // inclusive 1..cap-1

                if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
                    int current = state.getValue(SnowLayerBlock.LAYERS);
                    if (current >= cap) return;
                    int newLayers = Mth.clamp(current + step, 1, cap);
                    level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, newLayers),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    processed[0]++;
                } else if (level.isEmptyBlock(pos) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
                    int newLayers = Math.min(step, cap);
                    level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, newLayers),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    processed[0]++;
                }
            });
        }

        // Player-centric piling to preserve natural randomness near active players
        private static void doPlayerBasedPiling(ServerLevel level, ChunkPos chunkPos, int maxLayers, int budget) {
            final RandomSource rng = level.random;
            final int cap = Mth.clamp(maxLayers, 1, 8);

            // simple guard to avoid hammering the same columns in one pass
            final java.util.HashSet<Long> touchedColumns = new java.util.HashSet<>();

            int processed = 0;
            int viewDistChunks = Math.min(level.getServer().getPlayerList().getViewDistance(), 16);
            int radiusBlocks = Mth.clamp(viewDistChunks * 16, 16, 64);

            for (Map.Entry<ServerPlayer, BlockPos> e : playerPositions.entrySet()) {
                if (processed >= budget) break;

                BlockPos playerPos = e.getValue();
                for (int attempt = 0; attempt < MAX_ATTEMPTS && processed < budget; attempt++) {
                    int dx = rng.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;
                    int dz = rng.nextInt(radiusBlocks * 2 + 1) - radiusBlocks;
                    int x = playerPos.getX() + dx;
                    int z = playerPos.getZ() + dz;

                    // Restrict to current target chunk
                    if ((x >> 4) != chunkPos.x || (z >> 4) != chunkPos.z) continue;

                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);

                    // Column de-dup by (x,z)
                    long key = (((long) x) << 32) ^ (z & 0xffffffffL);
                    if (!touchedColumns.add(key)) continue;

                    // Basic validity checks
                    if (level.isEmptyBlock(pos.below())) continue;
                    if (level.getBlockState(pos.below()).is(Blocks.WATER)) continue;

                    // Vanilla cold check
                    if (!level.getBiome(pos).value().coldEnoughToSnow(pos)) continue;

                    BlockState state = level.getBlockState(pos);
                    int step = (cap <= 2) ? 1 : rng.nextInt(1, cap); // 1..cap-1 typically

                    if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
                        int current = state.getValue(SnowLayerBlock.LAYERS);
                        if (current >= cap) continue;
                        int newLayers = Mth.clamp(current + step, 1, cap);
                        level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, newLayers),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        processed++;
                    } else if (level.isEmptyBlock(pos) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
                        int newLayers = Math.min(step, cap);
                        level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, newLayers),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        processed++;
                    }
                }
            }
        }

        // When no players nearby: add a noisy random 1-4 layers across scattered spots in the chunk
        private static void applySingleNoisyLayer(ServerLevel level, ChunkPos chunkPos) {
            final RandomSource rng = level.random;
            final java.util.HashSet<Long> touchedColumns = new java.util.HashSet<>();

            // Try a limited scatter; only once per storm per chunk
            final int maxPlacements = 32;     // modest to avoid heavy ticks
            final int attempts = 96;          // more attempts than placements for noise
            final int[] placed = {0};

            sampleRandomChunkColumns(level, chunkPos, attempts, (pos, state) -> {
                if (placed[0] >= maxPlacements) return;

                // Uniqueness by (x,z)
                long key = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
                if (!touchedColumns.add(key)) return;

                if (level.isEmptyBlock(pos.below())) return;
                if (level.getBlockState(pos.below()).is(Blocks.WATER)) return;
                if (!level.getBiome(pos).value().coldEnoughToSnow(pos)) return;

                // Add 1-4 layers at this column if possible
                placeRandomLayersNoClimb(level, pos, 1, 4);
                placed[0]++;
            });
        }

        private static boolean isChunkNearAnyPlayer(ServerLevel level, ChunkPos chunkPos) {
            int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 16);
            for (BlockPos playerPos : playerPositions.values()) {
                ChunkPos p = new ChunkPos(playerPos);
                int dx = Math.abs(p.x - chunkPos.x);
                int dz = Math.abs(p.z - chunkPos.z);
                if (dx <= viewDist && dz <= viewDist) return true;
            }
            return false;
        }


        public static boolean isSnowStormAt(ServerLevel level, ChunkPos pos) {
            if (level instanceof ISnowStormLevel stormLevel) {
                return stormLevel.sereneseasonsplus$isSnowStormAt(pos);
            }
            return false; // safe default if mixin fails
        }

        public static int getSnowStormIntensity(ServerLevel level, ChunkPos pos) {
            if (level instanceof ISnowStormLevel stormLevel) {
                return stormLevel.sereneseasonsplus$getSnowStormIntensity(pos);
            }
            return 0; // default = no storm
        }


        /**
         * Finds a snow block in the given chunk by scanning a random column.
         * This is more efficient than random Y sampling, since it only checks
         * around the surface band where snow/ice can exist.
         *
         * @param level    the world
         * @param chunkPos the chunk to search
         * @return position of a snow block or null if none found
         */
        protected static BlockPos findRandomSnowBlockInChunk(Level level, ChunkPos chunkPos) {
            int minY = level.getMinBuildHeight();

            // pick a random column in the chunk
            int worldX = chunkPos.getMinBlockX() + RANDOM.nextInt(16);
            int worldZ = chunkPos.getMinBlockZ() + RANDOM.nextInt(16);

            // surface height from heightmap
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
            int startY = topY + 1;               // include snow above surface
            int endY = Math.max(minY, topY - 8); // scan a band below surface

            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            for (int y = startY; y >= endY; --y) {
                mutable.set(worldX, y, worldZ);
                BlockState state = level.getBlockState(mutable);
                if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)) {
                    return mutable.immutable();
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
        /**
         * Immediately removes snow and ice in the given chunk.
         * Uses the same iteration style as processChunk (min..max world coords).
         */
        protected static void clearSnowAndIce(ServerLevel level, ChunkPos chunkPos) {
            iterateChunkColumns(level, chunkPos, (pos, state) -> {
                if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                } else if (state.is(Blocks.ICE)) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            });
        }


        public static void handleOnChunkLoad(LevelChunk chunk, ServerLevel level) {
            if (tickCounter <= 500) return;
            ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
            // Persistence is handled via natural snow detection below; avoid platform-specific chunk data in common.

            ChunkPos chunkPos = chunk.getPos();
            var seasonState = SeasonHelper.getSeasonState(level);
            Season.SubSeason currentSeason = seasonState.getSubSeason();

            // Queue deep-winter initialization if needed
            LayerBounds deepWinterBounds = getDeepWinterLayerBounds(currentSeason, seasonState.getDay());
            if (deepWinterBounds != null && !tracked.sereneseasonsplus$hasAppliedInitialSnow()
                    && !chunkHasNaturalSnow(level, chunkPos)) {
                tracked.sereneseasonsplus$setShouldApplyInitialSnow(true);
                tracked.sereneseasonsplus$setNeedsSnowUpdate(true);
            }
            // First load or season changed
            if (tracked.sereneseasonsplus$getLastSeason() != currentSeason) {
                tracked.sereneseasonsplus$setLastSeason(currentSeason);
                tracked.sereneseasonsplus$setNeedsSnowUpdate(true);
            }

            // Rain change (platform-specific override handled via EnvironmentHelper)
            boolean isRaining = EnvironmentHelper.isRainning(level, chunkPos.getMiddleBlockPosition(65));
            if (isRaining != tracked.sereneseasonsplus$wasRaining()) {
                tracked.sereneseasonsplus$setWasRaining(isRaining);
                tracked.sereneseasonsplus$setNeedsSnowUpdate(true);
                if (!isRaining) {
                    tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);
                }
            }

            if (tracked.sereneseasonsplus$needsSnowUpdate()) {
                ChunkQueue.tryAdd(chunkPos, true);
                tracked.sereneseasonsplus$setNeedsSnowUpdate(false);
            }
        }

        private static boolean chunkHasNaturalSnow(ServerLevel level, ChunkPos chunkPos) {
            LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
            if (chunk instanceof ISnowTrackedChunk tracked) {
                return tracked.sereneseasonsplus$getSnowCount() > 0;
            }
            return false;
        }


        private static boolean applyDeepWinterInitialSnow(ServerLevel level, ChunkPos chunkPos, LayerBounds bounds) {
            BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
            int baseX = chunkPos.getMinBlockX();
            int baseZ = chunkPos.getMinBlockZ();
            int minHeight = level.getMinBuildHeight();

            int minLayers = Mth.clamp(bounds.minLayers(), 1, 8);
            int maxLayers = Mth.clamp(bounds.maxLayers(), minLayers, 8);
            RandomSource random = level.random;
            boolean placedAny = false;

            for (int dx = 0; dx < 16; ++dx) {
                for (int dz = 0; dz < 16; ++dz) {
                    int worldX = baseX + dx;
                    int worldZ = baseZ + dz;
                    int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
                    if (topY <= minHeight) continue;

                    placePos.set(worldX, topY, worldZ);
                    belowPos.set(worldX, topY - 1, worldZ);

                    BlockState belowState = level.getBlockState(belowPos);
                    if (belowState.isAir()) continue;
                    if (!belowState.getFluidState().isEmpty()) continue;
                    if (!level.getBiome(placePos).value().coldEnoughToSnow(placePos)) continue;

                    BlockState current = level.getBlockState(placePos);
                    if (!(current.isAir() || current.canBeReplaced())) continue;
                    if (current.is(Blocks.SNOW) || current.is(Blocks.SNOW_BLOCK)) continue;

                    BlockState snow = Blocks.SNOW.defaultBlockState();
                    if (!snow.canSurvive(level, placePos)) continue;

                    int layers = (maxLayers == minLayers)
                            ? minLayers
                            : random.nextInt(minLayers, maxLayers + 1);
                    layers = Mth.clamp(layers, minLayers, 8);

                    level.setBlock(placePos, snow.setValue(SnowLayerBlock.LAYERS, layers),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    placedAny = true;
                }
            }
            if (placedAny) {
                LevelChunk targetChunk = level.getChunk(chunkPos.x, chunkPos.z);
                ((ISnowTrackedChunk) targetChunk).sereneseasonsplus$incrementSnowCount();
            }

            return placedAny;
        }

        private static void markChunkWithInitialSnow(LevelChunk chunk) {
            // Avoid platform-specific persistent data in common; mark chunk dirty only.
            chunk.setUnsaved(true);
        }

        private static LayerBounds getDeepWinterLayerBounds(Season.SubSeason subSeason, int day) {
            return switch (subSeason) {
                case EARLY_WINTER -> (day >= 6) ? new LayerBounds(1, 3) : null;
                case MID_WINTER -> new LayerBounds(3, 5);
                case LATE_WINTER -> new LayerBounds(5, 7);
                case EARLY_SPRING -> (day < 4) ? new LayerBounds(1, Math.max(1, 5 - day)) : null;
                default -> null;
            };
        }

        /**
         * Reduces snow layers in a chunk to simulate a rapid warm-up.
         *
         * @param level    the world level to modify
         * @param chunkPos the chunk position to process
         */
        protected static void accelerateMelt(ServerLevel level, ChunkPos chunkPos, int budget) {
            final int[] processed = {0};

            iterateChunkColumns(level, chunkPos, (pos, state) -> {
                if (processed[0] >= budget) return;

                if (state.is(Blocks.SNOW_BLOCK)) {
                    level.setBlock(
                            pos,
                            Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4),
                            Block.UPDATE_ALL
                    );
                    processed[0]++;
                } else if (state.is(Blocks.SNOW)) {
                    int layers = state.getValue(BlockStateProperties.LAYERS);
                    int newLayers = layers - 3;
                    if (newLayers > 0) {
                        level.setBlock(
                                pos,
                                state.setValue(BlockStateProperties.LAYERS, newLayers),
                                Block.UPDATE_ALL
                        );
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    processed[0]++;
                } else if (state.is(Blocks.ICE)) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                    processed[0]++;
                }
            });
        }

        /**
         * Gradually melts snow in the given chunk.
         * Uses random column scanning for efficiency and consistency.
         *
         * @param level    the server level
         * @param chunkPos the chunk position to process
         */
    protected static void processMeltingChunk(ServerLevel level, ChunkPos chunkPos) {
            // Try melting up to 5 random columns in this chunk
            for (int i = 0; i < 5; ++i) {
                BlockPos snowPos = findRandomSnowBlockInChunk(level, chunkPos);
                if (snowPos == null) {
                    break;
                }
                SnowUtils.breakOrDecrementLayer(level, snowPos);
            }
    }


    public static void onServerStarting(int config, boolean snowStorm) {
            tickThresholdSnowReplacer = config;
            tickCounter = 0;
            playerPositions.clear();
            //LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
            tickCounter = 0;
            snowStormEnabled = snowStorm;
            Memory.erase();
            ChunkQueue.clear();

        }

        public static void onServerStopping() {
            playerPositions.clear();
            tickCounter = 0;
            ChunkQueue.clear();
            Memory.erase();
        }

        /**
         * Refreshes the tick threshold and async config on server tick,
         * allowing live config changes.
         */
        public static void onConfigReload(int config, boolean snowStorm) {
            tickThresholdSnowReplacer = config;
            snowStormEnabled = snowStorm;
        }

        /**
         * Called when the season changes. Proactively classifies nearby loaded chunks
         * around players into either full-clear queue or accelerated-melt queue based on
         * current biome temperature, so the world updates without requiring a reload.
         */
        public static void onSeasonChange(ServerLevel level) {
            var players = level.getServer().getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            ChunkQueue.clear();
            Memory.erase();
            ChunkMapInterfaceAccess chunkMap = (ChunkMapInterfaceAccess) level.getChunkSource().chunkMap;
            for (ChunkHolder chunk : chunkMap.snow$getChunksSafe()) {
                ChunkQueue.tryAdd(chunk.getPos(), false);
            }
        }
        /**
         * Places a snow layer at the given position if empty and valid, or adds a
         * layer to an existing snow block that is below max height.
         */
        public static void placeSnowAt(ServerLevel level, BlockPos pos) {
            // case 1: already snow with <8 layers → increment
            BlockPos.MutableBlockPos cursor = pos.mutable();
            BlockState current = level.getBlockState(cursor);
            int attempts = level.random.nextInt(2,MAX_ATTEMPTS);
            while (attempts-- > 0) {
                if (current.is(Blocks.SNOW) && current.hasProperty(SnowLayerBlock.LAYERS)) {
                    int layers = current.getValue(SnowLayerBlock.LAYERS);
                    if (layers < 8) {
                        // stack one more layer
                        level.setBlock(
                                cursor,
                                current.setValue(SnowLayerBlock.LAYERS, layers + 1),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                        );
                        return; // stop after success
                    }

                    // full block, check above
                    cursor.move(0, 1, 0);
                    current = level.getBlockState(cursor);
                    continue;
                }

                // empty block → place fresh snow with 1 layer
                else if (level.isEmptyBlock(cursor)) {
                    BlockState snow = Blocks.SNOW.defaultBlockState();
                    if (snow.canSurvive(level, cursor)) {
                        level.setBlock(
                                cursor,
                                snow.setValue(SnowLayerBlock.LAYERS, 1),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                        );
                    }
                    return; // stop after success
                }
                else{
                    cursor.move(0, 1, 0);
                    current = level.getBlockState(cursor);
                }
            }

        }

        // Exactly +1 layer at this column; never climbs above the sampled Y
        private static void placeSingleLayerNoClimb(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
                int layers = state.getValue(SnowLayerBlock.LAYERS);
                if (layers < 8) {
                    level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers + 1),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            } else if (level.isEmptyBlock(pos)) {
                BlockState snow = Blocks.SNOW.defaultBlockState();
                if (snow.canSurvive(level, pos)) {
                    level.setBlock(pos, snow.setValue(SnowLayerBlock.LAYERS, 1),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }

        // Add 1..N layers at this column; never climbs above the sampled Y
        private static void placeRandomLayersNoClimb(ServerLevel level, BlockPos pos, int minLayers, int maxLayers) {
            if (maxLayers < minLayers) {
                int t = maxLayers; maxLayers = minLayers; minLayers = t;
            }
            minLayers = Mth.clamp(minLayers, 1, 8);
            maxLayers = Mth.clamp(maxLayers, 1, 8);

            final RandomSource rng = level.random;
            final int add = (maxLayers == minLayers)
                    ? minLayers
                    : rng.nextInt(minLayers, maxLayers + 1); // inclusive

            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
                int layers = state.getValue(SnowLayerBlock.LAYERS);
                if (layers < 8) {
                    int newLayers = Mth.clamp(layers + add, 1, 8);
                    level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, newLayers),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            } else if (level.isEmptyBlock(pos)) {
                BlockState snow = Blocks.SNOW.defaultBlockState();
                if (snow.canSurvive(level, pos)) {
                    int startLayers = Mth.clamp(add, 1, 8);
                    level.setBlock(pos, snow.setValue(SnowLayerBlock.LAYERS, startLayers),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }

        private record LayerBounds(int minLayers, int maxLayers) {
        }

        public record WeatherDecision(Action action, Priority priority) {
        }

        public enum Action {
            SNOW,     // add snow layers
            MELT,     // reduce snow
            CLEAR,     // urgent wipe
            NONE      // do nothing
        }


    }

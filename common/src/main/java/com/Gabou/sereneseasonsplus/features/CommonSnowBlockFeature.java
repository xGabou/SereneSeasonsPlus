package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.features.snowstorm.ISnowStormLevel;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.tags.SSPTags;
import com.Gabou.sereneseasonsplus.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
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
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class CommonSnowBlockFeature {
    public static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    protected static final Random RANDOM = new Random();
    protected static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();
    protected static boolean snowStormEnabled = false;
    protected static int tickThresholdSnowReplacer;
    protected static int tickThresholdSnowReplacerForHotSeasons = 30;
    protected static int tickCounter = 0;
    public static SnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();


    protected static int snowStormIntensity = 0;

    protected static boolean isSnowStorming = false;

    // Ensures we catch up once for chunks that loaded during startup
    // before our queueing logic became active.
    private static boolean didStartupCatchUp = false;

    /**
     * Sampling attempts per call. Tune as desired.
     */
    protected static final int MAX_ATTEMPTS = 12;

    public static int getTickCounter() {
        return tickCounter;
    }


    // Classification lists for queued work
    static final List<BlockPos> snowMelt = new ArrayList<>();
    static final List<BlockPos> iceMelt = new ArrayList<>();
    static final List<BlockPos> snowPill = new ArrayList<>();

    // Precomputed changes and per-batch bookkeeping
    public record QueuedChange(BlockPos pos, BlockState state, int flags) {
    }

    static final Map<BlockPos, QueuedChange> pendingChanges = new LinkedHashMap<>();
    static final Set<ChunkPos> chunksToDirty = new HashSet<>();
    static final Map<ChunkPos, ChunkQueue.TaskType> chunkTaskTypes = new HashMap<>();
    static final Set<ChunkPos> chunksWithAppliedChanges = new HashSet<>();
    static int applyCycleTotal = 0;
    static int applyCycleProcessed = 0;

    public static void handleServerTick(MinecraftServer server, ServerLevel level) {
        if (level != null && !level.isClientSide()) {
            ++tickCounter;

            if (level.random.nextInt(16) == 0) {
                updatePlayerPositions(level.players());
                passifSnowBlocks(level);
            }

            // One-time catch-up: some chunks can load very early (before tick ~30)
            // and miss the on-load enqueue. Sweep nearby loaded chunks once and
            // enqueue initial snow so we don't leave unsnowed checkerboard patches.
//            if (!didStartupCatchUp && tickCounter > 40) {
//                performStartupCatchUp(level);
//                didStartupCatchUp = true;
//            }

            int phase = tickCounter % 5;
            if (phase == 0 && tickCounter > 10) return;
            if (phase == 1) {
                ChunkQueue.Entry entry;
                int processed = 0;
                if (ChunkQueue.isEmpty())
                    ChunkQueue.shuffle();

                while ((entry = ChunkQueue.poll()) != null) {
                    if ((!((MinecraftServerAccess) server).sereneseasonsplus$tempsEcoule() || processed >= 40) && processed >= 10) {
                        if (entry.type() == ChunkQueue.TaskType.APPLY_SNOW) {
                            enqueueChunkForSnowApply(entry.pos(), entry.subSeason());
                        } else {
                            enqueueChunkForSnowMelt(entry.pos(), entry.fullClear());
                        }
                        break;
                    }
                    boolean changed = false;
                    ChunkPos chunkPos = entry.pos();
                    if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                    if (chunk == null) {
                        continue;
                    }
                    ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
                    switch (entry.type()) {
                        case APPLY_SNOW -> {
                            int globalWinterId = EnvironmentHelper.getCurrentWinterId();
                            Season.SubSeason subSeason = EnvironmentHelper.getCurrentSeason();
                            if (tracked.sereneseasonsplus$getLastWinterId() != globalWinterId) {
                                tracked.sereneseasonsplus$setLastWinterId(globalWinterId);
                                tracked.sereneseasonsplus$setSnowCount(-1); // back to virgin state
                                tracked.sereneseasonsplus$setHasAppliedInitialSnow(false);
                                tracked.sereneseasonsplus$setShouldApplyInitialSnow(false);
                                tracked.sereneseasonsplus$willReceiveSnow(false);
                                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);
                                continue;
                            }
                            // Guard extra piling during active first snowfall
                            boolean isRainingNow = EnvironmentHelper.isRainning(level, chunkPos.getMiddleBlockPosition(65));
                            int sc = tracked.sereneseasonsplus$getSnowCount();
                            boolean allowPile = (sc < 1) || (sc > 1) || (sc == 1 && !isRainingNow);
                            if (!allowPile) {
                                enqueueChunkForSnowApplyWithSitting(chunkPos, subSeason, entry.sittingTicks());
                                tracked.sereneseasonsplus$willReceiveSnow(true);
                                break;
                            }

                            changed = applySnowToChunk(level, chunkPos, subSeason, level.getRandom());
                            if (changed) {
                                chunkTaskTypes.put(chunkPos, ChunkQueue.TaskType.APPLY_SNOW);
                                chunksToDirty.add(chunkPos);
                                chunksWithAppliedChanges.add(chunkPos);
                            } else {
                                if (tracked.sereneseasonsplus$getSnowCount() <= 0 && entry.sittingTicks() < 20) {
                                    // virgin chunk → try again later
                                    tracked.sereneseasonsplus$willReceiveSnow(true);
                                    enqueueChunkForSnowApplyWithSitting(chunkPos, subSeason, entry.sittingTicks());
                                } else {
                                    // already had snow before → mark handled
                                    tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);
                                    tracked.sereneseasonsplus$willReceiveSnow(false);
                                    ChunkQueue.enqueueBugged(chunkPos, subSeason);
                                }

                            }

                        }
                        case MELT_SNOW -> {
                            changed = meltSnowInChunk(level, chunkPos, entry.fullClear());
                            if (changed) {
                                chunkTaskTypes.put(chunkPos, ChunkQueue.TaskType.MELT_SNOW);
                                chunksToDirty.add(chunkPos);
                            }
                        }
                    }
                    processed++;

                }
            }

            // Apply queued changes on ticks 2, 3, and 4; skip tick 5
            if (phase == 2 || phase == 3 || phase == 4) {
                if (applyCycleProcessed == 0) {
                    applyCycleTotal = pendingChanges.size();
                }
                int batch = (applyCycleTotal + 2) / 3;
                int toProcess = (phase == 4) ? Integer.MAX_VALUE : batch;
                int applied = processQueuedChanges(level, toProcess);
                applyCycleProcessed += applied;
                if (phase == 4) {
                    finalizeChunkBatch(level);
                    applyCycleTotal = 0;
                    applyCycleProcessed = 0;
                }
            }
        }
    }

    public static void handleOnChunkLoad(LevelChunk chunk, ServerLevel level) {
        counterTime++;
        ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
        if (tracked == null) return;

        ChunkPos chunkPos = chunk.getPos();
        var seasonState = SeasonHelper.getSeasonState(level);
        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();

        if (seasonState == null || currentSeason == null) {
            ChunkQueue.enqueueScheduled(chunkPos);
            return;
        }

        SnowLogic.evaluate(level, currentSeason, seasonState, tracked, chunkPos, true, chunk.getHeight());
    }


    public static void enqueueChunkForSnowApply(ChunkPos chunkPos, Season.SubSeason subSeason) {
        ChunkQueue.enqueueApply(chunkPos, subSeason);
    }

    /**
     * Enqueue with sitting ticks to avoid busy looping on virgin chunks that have no snow yet.
     * already incremented by one to avoid 0 sitting ticks.
     *
     * @param chunkPos  the chunk position
     * @param subSeason the sub season
     * @param sitting   the sitting ticks
     */
    public static void enqueueChunkForSnowApplyWithSitting(ChunkPos chunkPos, Season.SubSeason subSeason, int sitting) {
        ChunkQueue.enqueueApplyWithSitting(chunkPos, subSeason, sitting + 1);
    }

    public static void enqueueChunkForSnowMelt(ChunkPos chunkPos, boolean fullClear) {
        ChunkQueue.enqueueMelt(chunkPos, fullClear);
    }


    public static boolean applySnowToChunk(ServerLevel level,
                                           ChunkPos chunkPos,
                                           Season.SubSeason subSeason,
                                           RandomSource random) {

        int day = SeasonHelper.getSeasonState(level).getDay();

        LayerBounds bounds = getSeasonalLayerBounds(subSeason, day);
        boolean[] changed = {false}; // workaround for lambda capture
        RandomSource columnRandom = RandomSource.create(chunkPos.toLong() ^ level.getSeed() ^ random.nextLong());

        iterateChunkColumns(level, chunkPos, (pos, state) -> {
            BlockPos belowPos = pos.below();
            // Try to place snow at this column top
            if (tryPlaceSnow(level, pos, belowPos, subSeason, day, columnRandom, bounds)) {
                changed[0] = true;
            }
        });

        return changed[0];
    }


    private static boolean tryPlaceSnow(ServerLevel level,
                                        BlockPos surfacePos,
                                        BlockPos belowPos,
                                        Season.SubSeason subSeason,
                                        int day,
                                        RandomSource columnRandom,
                                        LayerBounds bounds) {
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.isAir() || !belowState.getFluidState().isEmpty()) return false;
        BlockState current = level.getBlockState(surfacePos);
        if (current.canBeReplaced()) {
            return queueSnowLayersIfNeeded(level, surfacePos, 1, true);
        }
        if (!Blocks.SNOW.defaultBlockState().canSurvive(level, surfacePos)) return false;

        // wave + jitter logic
        double wave = Math.sin((surfacePos.getX() * 0.12D) + (surfacePos.getZ() * 0.12D) + (day * 0.05D));
        double normalized = (wave + 1.0D) * 0.5D; // 0..1
        int range = bounds.maxLayers() - bounds.minLayers();
        int baseLayers = bounds.minLayers() + (range > 0 ? (int) Math.round(normalized * range) : 0);
        baseLayers = Mth.clamp(baseLayers, bounds.minLayers(), bounds.maxLayers());

        int jitter = columnRandom.nextInt(3) - 1; // -1..1
        int targetLayers = Mth.clamp(baseLayers + jitter, 1, 8);

        if (current.is(Blocks.SNOW_BLOCK)) {
            targetLayers = 8;
        } else if (current.is(Blocks.SNOW) && current.hasProperty(SnowLayerBlock.LAYERS)) {
            int existing = current.getValue(SnowLayerBlock.LAYERS);
            targetLayers = Math.max(existing, targetLayers);
        }

        return queueSnowLayersIfNeeded(level, surfacePos, targetLayers, true);
    }


    public static boolean meltSnowInChunk(ServerLevel level, ChunkPos chunkPos, boolean fullClear) {
        if (fullClear) {
            collectClearSnowAndIce(level, chunkPos);
            return true;
        }

        final RandomSource random = level.getRandom();
        final boolean[] any = {false};
        iterateChunkColumns(level, chunkPos, (pos, state) -> {
            if (state.is(Blocks.SNOW_BLOCK)) {
                if (random.nextBoolean()) {
                    any[0] |= queueClearIfNeeded(level, pos, false);
                } else {
                    any[0] |= queueSnowLayersIfNeeded(level, pos, 4, false);
                }
            } else if (state.is(Blocks.SNOW)) {
                int layers = state.getValue(BlockStateProperties.LAYERS);
                int drop = 1 + random.nextInt(2);
                int newLayers = Math.max(0, layers - drop);
                if (newLayers <= 0) {
                    any[0] |= queueClearIfNeeded(level, pos, false);
                } else {
                    any[0] |= queueSnowLayersIfNeeded(level, pos, newLayers, false);
                }
            } else if (state.is(Blocks.ICE) && random.nextFloat() < 0.35F) {
                any[0] |= queueClearIfNeeded(level, pos, true);
            }
        });
        return any[0];
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
            if (blocksToReplace < 0) {

                if (!EnvironmentHelper.isRainning(level, playerPos)) return;

                final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
                final RandomSource random = level.random;
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
            } else {
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


    private static void iterateChunkColumns(ServerLevel level, ChunkPos
            chunkPos, BiConsumer<BlockPos.MutableBlockPos, BlockState> action) {
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

    private static void columnsLogic(ServerLevel level, BiConsumer<BlockPos.MutableBlockPos, BlockState> action,
                                     int worldX, int worldZ, int minY, BlockPos.MutableBlockPos mutable) {
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

    private static void checkHeight(ServerLevel level, BiConsumer<BlockPos.MutableBlockPos, BlockState> action,
                                    int worldX, int worldZ, int minY, BlockPos.MutableBlockPos mutable, int nlTopY) {
        int nlStart = nlTopY + 1;
        int nlEnd = Math.max(minY, nlTopY - 8);

        for (int y = nlStart; y >= nlEnd; --y) {
            mutable.set(worldX, y, worldZ);
            BlockState state = level.getBlockState(mutable);
            action.accept(mutable, state);
        }
    }

    private static void sampleRandomChunkColumns(ServerLevel level, ChunkPos chunkPos, int attempts, BiConsumer<
            BlockPos.MutableBlockPos, BlockState> action) {
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
     * Immediately removes snow and ice in the given chunk.
     *
     * @param level    level to modify
     * @param chunkPos target chunk position
     */
    /**
     * Immediately removes snow and ice in the given chunk using a full column scan.
     */
    protected static void collectClearSnowAndIce(ServerLevel level, ChunkPos chunkPos) {
        iterateChunkColumns(level, chunkPos, (pos, state) -> {
            if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                queueClearIfNeeded(level, pos, false);
            } else if (state.is(Blocks.ICE)) {
                queueClearIfNeeded(level, pos, true);
            }
        });
    }


    public static int counterTime = 0;

    /**
     * Sweep loaded chunks around players once shortly after startup and
     * enqueue any that missed the initial on-load window.
     */
//    private static void performStartupCatchUp(ServerLevel level) {
//        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
//        int day = SeasonHelper.getSeasonState(level).getDay();
//        LayerBounds bounds = getSeasonalLayerBounds(currentSeason, day);
//        ChunkSource chunkSource = level.getChunkSource();
//
//        for (ServerPlayer player : level.players()) {
//            BlockPos center = player.blockPosition();
//            int view = getSimulationDistance(player); // chunks
//            int pcx = center.getX() >> 4;
//            int pcz = center.getZ() >> 4;
//
//            for (int dx = -view; dx <= view; dx++) {
//                for (int dz = -view; dz <= view; dz++) {
//                    int cx = pcx + dx;
//                    int cz = pcz + dz;
//                    ChunkAccess access = chunkSource.getChunk(cx, cz, false);
//                    if (!(access instanceof LevelChunk lc)) continue;
//                    ISnowTrackedChunk tracked = (ISnowTrackedChunk) lc;
//                    if (bounds != null) {
//                        boolean pending = HANDLER.shouldApplySnow(level, lc.getPos());
//                        boolean hasSnow = tracked.sereneseasonsplus$getSnowCount() > 0;
//                        if (!pending && !hasSnow) {
//                            continue;
//                        }
//                        if (hasSnow && tracked.sereneseasonsplus$hasReceivedSnowLayerThisStorm()) {
//                            continue; // already processed for this storm
//                        }
//                        if (pending) {
//                            tracked.sereneseasonsplus$setShouldApplyInitialSnow(true);
//                            tracked.sereneseasonsplus$willReceiveSnow(true);
//                        }
//                        boolean isRainingHere = EnvironmentHelper.isRainning(level, lc.getPos().getMiddleBlockPosition(65));
//                        int sc = tracked.sereneseasonsplus$getSnowCount();
//                        boolean allowPile = (sc < 1) || (sc > 1) || (sc == 1 && !isRainingHere);
//                        if (allowPile) {
//                            enqueueChunkForSnowApply(lc.getPos(), currentSeason);
//                        }
//                    } else {
//                        enqueueChunkForSnowMelt(lc.getPos(), true);
//
//                    }
//
//                }
//            }
//        }
//    }

//        private static boolean chunkHasNaturalSnow(ServerLevel level, ChunkPos chunkPos) {
//            LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
//            if (chunk instanceof ISnowTrackedChunk tracked) {
//                return tracked.sereneseasonsplus$getSnowCount() > 0;
//            }
//            return false;
//        }


    public static LayerBounds getSeasonalLayerBounds(Season.SubSeason subSeason, int day) {
        return switch (subSeason) {
            case EARLY_WINTER -> (day >= 6) ? new LayerBounds(1, 3) : null;
            case MID_WINTER -> new LayerBounds(3, 5);
            case LATE_WINTER -> new LayerBounds(5, 7);
            case EARLY_SPRING -> (day < 4) ? new LayerBounds(1, Math.max(1, 5 - day)) : null;
            default -> new LayerBounds(1, 5);
        };
    }


    public static void onServerStarting(int config, boolean snowStorm) {
        tickThresholdSnowReplacer = config;
        tickCounter = 0;
        playerPositions.clear();
        //LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
        tickCounter = 0;
        snowStormEnabled = snowStorm;
        ChunkQueue.clear();
        counterTime = 0;

    }

    public static void onServerStopping() {
        playerPositions.clear();
        tickCounter = 0;
        ChunkQueue.clear();
        counterTime = 0;
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
        ChunkQueue.clear();
        // Reset pending flags on loaded chunks so tick hook can re-enqueue
        ChunkSource source = level.getChunkSource();
        for (ServerPlayer player : level.players()) {
            int view = getSimulationDistance(player);
            ChunkPos pc = new ChunkPos(player.blockPosition());
            for (int dx = -view; dx <= view; dx++) {
                for (int dz = -view; dz <= view; dz++) {
                    LevelChunk access = source.getChunk(pc.x + dx, pc.z + dz, false);
                    if (access == null) continue;
                    ISnowTrackedChunk tracked = (ISnowTrackedChunk) access;
                    tracked.sereneseasonsplus$willReceiveSnow(false);
                    tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);

                }
            }
        }
//        performStartupCatchUp(level);

    }

    // Exactly +1 layer at this column; never climbs above the sampled Y
    private static void placeSingleLayerNoClimb(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) {
                setSnowLayersIfChanged(level, pos, layers + 1, false);
            }
        } else if (level.isEmptyBlock(pos)) {
            BlockState snow = Blocks.SNOW.defaultBlockState();
            if (snow.canSurvive(level, pos)) {
                setSnowLayersIfChanged(level, pos, 1, true);
            }
        }
    }

    // Add 1..N layers at this column; never climbs above the sampled Y
    private static void placeRandomLayersNoClimb(ServerLevel level, BlockPos pos, int minLayers, int maxLayers) {
        if (maxLayers < minLayers) {
            int t = maxLayers;
            maxLayers = minLayers;
            minLayers = t;
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
                setSnowLayersIfChanged(level, pos, newLayers, false);
            }
        } else if (level.isEmptyBlock(pos)) {
            BlockState snow = Blocks.SNOW.defaultBlockState();
            if (snow.canSurvive(level, pos)) {
                int startLayers = Mth.clamp(add, 1, 8);
                setSnowLayersIfChanged(level, pos, startLayers, true);
            }
        }
    }

    public record LayerBounds(int minLayers, int maxLayers) {
    }

    /**
     * Sets snow layers only if the final state would differ.
     * Returns true if a block state changed.
     */
    private static boolean setSnowLayersIfChanged(ServerLevel level, BlockPos pos, int targetLayers,
                                                  boolean allowPlace) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            int current = state.getValue(SnowLayerBlock.LAYERS);
            if (current == targetLayers) {
                return false; // no change
            }
            BlockState newState = state.setValue(SnowLayerBlock.LAYERS, targetLayers);
            // lighter flags: client update only
            return level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        } else if (allowPlace && (level.isEmptyBlock(pos) || state.canBeReplaced())) {
            BlockState snow = Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, Mth.clamp(targetLayers, 1, 8));
            if (!snow.canSurvive(level, pos)) return false;
            return level.setBlock(pos, snow, Block.UPDATE_CLIENTS);
        }

        return false;
    }

    // Queueing variants: compute and enqueue desired state without immediate set
    private static boolean queueSnowLayersIfNeeded(ServerLevel level, BlockPos pos, int targetLayers,
                                                   boolean allowPlace) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            int current = state.getValue(SnowLayerBlock.LAYERS);
            if (current == targetLayers) return false;
            BlockState newState = state.setValue(SnowLayerBlock.LAYERS, Mth.clamp(targetLayers, 1, 8));
            queueChange(pos, newState, Block.UPDATE_CLIENTS);
            snowPill.add(pos.immutable());
            return true;
        } else if (allowPlace && (level.isEmptyBlock(pos) || state.canBeReplaced())) {
            BlockState snow = Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, Mth.clamp(targetLayers, 1, 8));
            if (!snow.canSurvive(level, pos)) return false;
            queueChange(pos, snow, Block.UPDATE_CLIENTS);
            snowPill.add(pos.immutable());
            return true;
        }
        return false;
    }

    /**
     * Sets the block state only if it would change. Returns true if changed.
     * Optionally includes known-shape update flag.
     */
    private static boolean setBlockIfDifferent(ServerLevel level, BlockPos pos, BlockState wanted,
                                               boolean knownShape) {
        BlockState current = level.getBlockState(pos);
        if (current.equals(wanted)) return false;
        int flags = Block.UPDATE_CLIENTS | (knownShape ? Block.UPDATE_KNOWN_SHAPE : 0);
        return level.setBlock(pos, wanted, flags);
    }

    /**
     * Clears snow/ice only if the state would actually change.
     * Returns true if changed.
     */
    private static boolean clearIfChanged(ServerLevel level, BlockPos pos, boolean toWater) {
        BlockState wanted = toWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockState current = level.getBlockState(pos);
        if (current.is(wanted.getBlock())) return false; // already correct
        return level.setBlock(pos, wanted, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
    }

    private static boolean queueClearIfNeeded(ServerLevel level, BlockPos pos, boolean toWater) {
        BlockState wanted = toWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockState current = level.getBlockState(pos);
        if (current.is(wanted.getBlock())) return false;
        queueChange(pos, wanted, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        if (toWater) {
            iceMelt.add(pos.immutable());
        } else {
            snowMelt.add(pos.immutable());
        }
        return true;
    }

    private static void queueChange(BlockPos pos, BlockState state, int flags) {
        BlockPos imm = pos.immutable();
        pendingChanges.put(imm, new QueuedChange(imm, state, flags));
    }

    private static int processQueuedChanges(ServerLevel level, int limit) {
        if (pendingChanges.isEmpty()) return 0;
        int applied = 0;
        Iterator<Map.Entry<BlockPos, QueuedChange>> it = pendingChanges.entrySet().iterator();
        while (it.hasNext() && applied < limit) {
            Map.Entry<BlockPos, QueuedChange> e = it.next();
            QueuedChange qc = e.getValue();
            level.setBlock(qc.pos(), qc.state(), qc.flags());
            it.remove();
            applied++;
        }
        return applied;
    }

    private static void finalizeChunkBatch(ServerLevel level) {
        for (ChunkPos cp : chunksToDirty) {
            if (!level.hasChunk(cp.x, cp.z)) continue;
            LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
            if (chunk == null) continue;
            chunk.setUnsaved(true);
            ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
            ChunkQueue.TaskType type = chunkTaskTypes.get(cp);
            if (type == ChunkQueue.TaskType.APPLY_SNOW) {
                tracked.sereneseasonsplus$setHasAppliedInitialSnow(true);
                tracked.sereneseasonsplus$setShouldApplyInitialSnow(false);
                tracked.sereneseasonsplus$incrementSnowCount();
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(true);
                tracked.sereneseasonsplus$willReceiveSnow(false);
                HANDLER.onSnowApplied(level, cp, chunksWithAppliedChanges.contains(cp));
            } else if (type == ChunkQueue.TaskType.MELT_SNOW) {
                tracked.sereneseasonsplus$setHasAppliedInitialSnow(false);
                tracked.sereneseasonsplus$setShouldApplyInitialSnow(false);
                tracked.sereneseasonsplus$setSnowCount(0);
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);
                tracked.sereneseasonsplus$willReceiveSnow(false);
            }
        }
        chunksToDirty.clear();
        chunkTaskTypes.clear();
        chunksWithAppliedChanges.clear();
        snowMelt.clear();
        iceMelt.clear();
        snowPill.clear();
    }


}

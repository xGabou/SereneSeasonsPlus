package com.Gabou.sereneseasonsplus.features;


import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.features.logic.SnowAccumulationPolicy;
import com.Gabou.sereneseasonsplus.features.snowstorm.ISnowStormLevel;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import net.Gabou.gaboulibs.storage.SnowRecord;
import com.Gabou.sereneseasonsplus.tags.SSPTags;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.util.MinecraftServerAccess;
import net.Gabou.gaboulibs.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
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

public class CommonSnowBlockFeature {

    public static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");

    protected static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();

    protected static int tickThresholdSnowReplacer;
    protected static int tickCounter = 0;

    protected static boolean needUpdateSnowFeature = false;


    public static ISnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();
    public static final SnowBlockCompatibility SNOW_COMPATIBILITY = new AdaptiveSnowBlockCompatibility();
    protected static final SnowHistoryQueryService HISTORY_QUERY_SERVICE = new SnowHistoryQueryService();
    protected static final SnowStateService SNOW_STATE_SERVICE = new SnowStateService();
    public static final SnowAccumulationPolicy SNOW_ACCUMULATION_POLICY =
            new SnowAccumulationPolicy(HISTORY_QUERY_SERVICE, SNOW_STATE_SERVICE);
    protected static final SnowChunkApplyService CHUNK_APPLY_SERVICE = new SnowChunkApplyService(HISTORY_QUERY_SERVICE, SNOW_STATE_SERVICE);
    protected static final SnowChunkMeltService CHUNK_MELT_SERVICE = new SnowChunkMeltService(SNOW_STATE_SERVICE);
    protected static final ActiveSnowUpdateService ACTIVE_SNOW_UPDATE_SERVICE = new ActiveSnowUpdateService();
    protected static final SnowMutationBatch MUTATION_BATCH = new SnowMutationBatch();
    protected static final SnowChunkLoadReconciler LOAD_RECONCILER = new SnowChunkLoadReconciler(SNOW_STATE_SERVICE);

    protected static final int MAX_ATTEMPTS = 64;

    // restored for visibility or metrics during a batch
    static final List<BlockPos> snowPill = new ArrayList<>();

    static int applyCycleTotal = 0;
    static int applyCycleProcessed = 0;


    public static int getSnowHeightCap() {
        return maxHeightForSnow;
    }

    protected static boolean snowFeatureEnabled = false;


    public static boolean isSnowFeatureEnabled() {
        return snowFeatureEnabled;
    }
    // Piling speed controls for active storms
    // When true, use immediate/fast piling (current behavior). When false, pile gradually.
    public static boolean FAST_PILING_MODE = false;
    // Target time to reach full storm distribution (in ticks). Default ~8000 per request.
    public static int ACTIVE_STORM_TARGET_TICKS = 8000;
    // Multiplier to scale the speed (1.0 = default; >1 faster, <1 slower)
    public static float STORM_INTENSITY_MULTIPLIER = 1.0f;

    protected static int maxHeightForSnow;

    public static void setFastPilingMode(boolean enabled) {
        FAST_PILING_MODE = enabled;
    }

    public static void setActiveStormTargetTicks(int ticks) {
        ACTIVE_STORM_TARGET_TICKS = Math.max(1, ticks);
    }

    public static void setStormIntensityMultiplier(float mult) {
        STORM_INTENSITY_MULTIPLIER = Math.max(0.01f, mult);
    }

    public static int getTickCounter() {
        return tickCounter;
    }


    protected static void clear() {
        playerPositions.clear();
        tickCounter = 0;
        ChunkQueue.clear();
        MUTATION_BATCH.clear();
        LOAD_RECONCILER.clear();
        snowPill.clear();
        applyCycleTotal = 0;
        applyCycleProcessed = 0;
    }

    public static void handleServerTick(MinecraftServer server, ServerLevel level) {
        if (level == null || level.isClientSide()) return;

        ++tickCounter;
        ChunkQueue.setCurrentTick(tickCounter);

        if (!snowFeatureEnabled) {
            clear();
            return;
        }

        if (needUpdateSnowFeature) {
            server.getWorldData().getGameRules()
                    .set(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT, maxHeightForSnow, server);
            needUpdateSnowFeature = false;
        }

        if (LOAD_RECONCILER.hasPendingLoads()) {
            chunkHandler(level);
        }

        if (level.random.nextInt(16) == 0 || (EnvironmentHelper.isHotSeason() && level.random.nextInt(2) == 0)) {
            updatePlayerPositions(level.players());
            passifSnowBlocks(level);
            EnvironmentHelper.checkAndUpdate(level);
        }

        int phase = tickCounter % 5;
        if (phase == 0 && tickCounter > 10) return;

        if (phase == 1) {
            ChunkQueue.Entry entry;
            int processed = 0;
            int inspected = 0;

            if (ChunkQueue.isEmpty()) ChunkQueue.shuffle();

            while ((entry = ChunkQueue.poll()) != null) {
                inspected++;
                boolean timeUp = (
                        ((MinecraftServerAccess) server).sereneseasonsplus$tempsEcoule() && processed >= 5
                ) || processed >= 20 || inspected > 60;
                if (timeUp) {
                    if (entry.type() == ChunkQueue.TaskType.APPLY_SNOW) {
                        enqueueChunkForSnowApply(entry.pos(), entry.subSeason());
                    } else if (entry.type() == ChunkQueue.TaskType.MELT_SNOW) {
                        enqueueChunkForSnowMelt(entry.pos(), entry.fullClear());
                    } else {
                        ChunkQueue.requeueDeferred(entry);
                    }
                    break;
                }

                boolean changed = false;
                ChunkPos chunkPos = entry.pos();
                if (!hasRequiredNeighborChunks(level, chunkPos)) {
                    if (entry.attempts() < ChunkQueue.MAX_DEFER_ATTEMPTS) {
                        ChunkQueue.requeueDeferred(entry);
                    } else {
                        ChunkQueue.markDropped(entry);
                    }
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                if (chunk == null) {
                    if (entry.attempts() < ChunkQueue.MAX_DEFER_ATTEMPTS) {
                        ChunkQueue.requeueDeferred(entry);
                    } else {
                        ChunkQueue.markDropped(entry);
                    }
                    continue;
                }

                switch (entry.type()) {
                    case APPLY_SNOW -> {
                        boolean synced = syncTrackedColumnsToWorld(level, chunk);
                        if (!synced) {
                            synced = applySnowHistoryPass(level, chunk);
                        }
                        if (!synced) {
                            synced = applySnowPatternFromActiveRecord(level, chunk);
                        }
                        if (synced) {
                            MUTATION_BATCH.markChunkDirty(chunkPos);
                            changed = true;
                        }
                    }
                    case MELT_SNOW -> {
                        changed = meltSnowInChunk(level, chunkPos, entry.fullClear());
                        if (changed) {
                            MUTATION_BATCH.markChunkDirty(chunkPos);
                        }
                    }
                }
                ChunkQueue.markProcessed(entry);
                processed++;
            }
        }

        if (phase == 2 || phase == 3 || phase == 4) {
            if (applyCycleProcessed == 0) {
                applyCycleTotal = MUTATION_BATCH.pendingChangeCount();
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

    // On chunk load, only cache surface height; do not enqueue or modify snow lists
    public static void handleOnChunkLoad(LevelChunk chunk) {
        if (isSnowFeatureEnabled()) {
            LOAD_RECONCILER.enqueue(chunk);
        }
    }

    protected static void chunkHandler(ServerLevel level) {
        LOAD_RECONCILER.process(level, SNOW_COMPATIBILITY);
    }

    public static void enqueueChunkForSnowApply(ChunkPos chunkPos, Season.SubSeason subSeason) {
        ChunkQueue.enqueueApply(chunkPos, subSeason);
    }

    public static void enqueueChunkForSnowMelt(ChunkPos chunkPos, boolean fullClear) {
        ChunkQueue.enqueueMelt(chunkPos, fullClear);
    }

    protected static void passifSnowBlocks(ServerLevel level) {
        ACTIVE_SNOW_UPDATE_SERVICE.run(level, playerPositions);
    }

    protected static boolean syncTrackedColumnsToWorld(ServerLevel level, LevelChunk chunk) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        ChunkPos cp = chunk.getPos();

        // 🚩 if we have pending changes for this chunk, skip sync entirely
        if (MUTATION_BATCH.hasPendingWorkForChunk(cp)) {
            return false;
        }

        Map<BlockPos, Integer> columns = tracked.sereneseasonsplus$getSnowColumns();
        if (columns == null) return false;

        if (columns.isEmpty()) return false;

        boolean changed = false;

        for (Map.Entry<BlockPos, Integer> e : new ArrayList<>(columns.entrySet())) {
            BlockPos pos = e.getKey();
            int wantedLayers = Mth.clamp(e.getValue(), 0, 8);

            if (wantedLayers <= 0) {
                changed |= queueClearIfNeeded(level, pos, false);
                columns.remove(pos);
                continue;
            }

            changed |= placeOrQueueLayers(level, pos, wantedLayers, true, true);
        }
        return changed;
    }

    private static boolean hasRequiredNeighborChunks(ServerLevel level, ChunkPos chunkPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(chunkPos.x + dx, chunkPos.z + dz)) {
                    return false;
                }
            }
        }
        return true;
    }


    // Enforce baseline per-column from finished storms; if nothing to do, optionally add bias from active storm
    protected static boolean applySnowHistoryPass(ServerLevel level, LevelChunk chunk) {
        return CHUNK_APPLY_SERVICE.applySnowHistoryPass(level, chunk);
    }

    // Attempts to freeze a water block at pos if conditions are met. Returns true if a block changed.
    public static boolean tryFreezeWaterAt(ServerLevel level, BlockPos pos) {
        if (pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER)) return false;

        BlockPos sample = pos.above();
        // Do not freeze under roofs/caves
        if (!isExposedToSky(level, sample)) return false;
        if (!HANDLER.isColdEnoughForSnow(level, sample)) return false;
        if (!isWaterBiome(level, pos)) return false;

        BlockState ice = Blocks.ICE.defaultBlockState();
        if (level.setBlockAndUpdate(pos, ice)) {
            accumulateColumnUpdate(level, pos, ice);
            return true;
        }
        return false;
    }

    protected static boolean isWaterBiome(ServerLevel level, BlockPos pos) {
        try {
            Holder<net.minecraft.world.level.biome.Biome> holder = level.getBiome(pos);
            return holder.unwrapKey()
                    .map(key -> {
                        String id = key.toString();
                        return id.contains("ocean") || id.contains("river");
                    })
                    .orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }

    protected static boolean applySnowPatternFromActiveRecord(ServerLevel level, LevelChunk chunk) {
        return CHUNK_APPLY_SERVICE.applySnowPatternFromActiveRecord(level, chunk);
    }

    protected static boolean applySnowPattern(ServerLevel level,
                                              LevelChunk chunk,
                                              SnowRecord record,
                                              RandomSource rng) {
        return CHUNK_APPLY_SERVICE.applySnowPattern(level, chunk, record, rng);
    }

    // Combined pattern from finished storms: also respect cap
    protected static boolean applyCombinedFinishedPattern(ServerLevel level,
                                                          LevelChunk chunk,
                                                          SnowRecord combined,
                                                          RandomSource rng) {
        return CHUNK_APPLY_SERVICE.applyCombinedFinishedPattern(level, chunk, combined, rng);
    }

    // Checks whether any snow column inside the chunk has total layers >= cap
    protected static boolean isChunkAtOrAboveSnowCap(ServerLevel level, LevelChunk chunk, int capLayers) {
        return CHUNK_APPLY_SERVICE.isChunkAtOrAboveSnowCap(level, chunk, capLayers);
    }


    public static boolean meltSnowInChunk(ServerLevel level, ChunkPos chunkPos, boolean fullClear) {
        return CHUNK_MELT_SERVICE.meltSnowInChunk(level, chunkPos, fullClear);
    }

    protected static boolean clearCoveredMeltablesNearSurface(ServerLevel level, LevelChunk chunk) {
        return CHUNK_MELT_SERVICE.clearCoveredMeltablesNearSurface(level, chunk);
    }

    protected static boolean meltTrackedIce(ServerLevel level, ISnowTrackedChunk tracked) {
        return CHUNK_MELT_SERVICE.meltTrackedIce(level, tracked);
    }

    // surface selection
    protected static BlockPos findPlacementTop(ServerLevel level, int x, int z) {
        return SnowColumnInspector.findPlacementTop(level, x, z, SNOW_COMPATIBILITY);
    }

    protected static boolean isExposedToSky(ServerLevel level, BlockPos pos) {
        if (pos.equals(new BlockPos(-98, 63, -104))) {
            int debug = 0;
        }
        try {
            return level.canSeeSkyFromBelowWater(pos);
        } catch (Throwable t) {
            return true; // fail open to avoid breaking placement if method differs
        }
    }

    // Placement helper: considers leaf canopies as pass-through for snowfall
    protected static boolean canReceiveSnowAt(ServerLevel level, BlockPos pos) {
        try {
            if (level.canSeeSkyFromBelowWater(pos)) return true;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
            int max = level.getMaxY();
            while (cursor.getY() < max) {
                BlockState st = level.getBlockState(cursor);
                if (st.isAir()) {
                    cursor.move(0, 1, 0);
                    continue;
                }
                // Treat leaves as letting snow through
                if (st.is(net.minecraft.tags.BlockTags.LEAVES)) {
                    cursor.move(0, 1, 0);
                    continue;
                }
                // Found a solid cover: cannot receive snowfall
                return false;
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Unified setter
     * If queue is true we enqueue using the classic queueSnowLayersIfNeeded
     * If queue is false we set immediately
     */
    protected static boolean placeOrQueueLayers(ServerLevel level, BlockPos pos, int targetLayers, boolean allowPlace, boolean queue) {
        // Skip placement if this column was destroyed during the current storm
        com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData sd = com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData.get();
        int activeId = (sd != null) ? sd.currentStormId : 0;
        if (activeId > 0) {
            net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            if (chunk instanceof com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk tracked) {
                if (tracked.sereneseasonsplus$getDestroyedStormId() != activeId) {
                    // Different storm than what was recorded: reset tracking lazily
                    tracked.sereneseasonsplus$getDestroyedColumns().clear();
                    tracked.sereneseasonsplus$setDestroyedStormId(activeId);
                }
                long xz = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
                if (tracked.sereneseasonsplus$getDestroyedColumns().contains(xz)) {
                    return false;
                }
            }
        }
        if (queue) {
            return queueSnowLayersIfNeeded(level, pos, targetLayers, allowPlace);
        }

        targetLayers = Mth.clamp(targetLayers, 1, 8);
        BlockState state = level.getBlockState(pos);

        // Enforce per-column cap based on gamerule/maxSnowHeight
        targetLayers = clampLayersForColumnCap(level, pos, state, targetLayers);
        if (targetLayers <= 0) return false;

        int current = SNOW_COMPATIBILITY.getManagedLayers(level, pos, state);
        targetLayers = Mth.clamp(targetLayers, 1, Math.max(1, SNOW_COMPATIBILITY.getMaxManagedLayers(level, pos, state)));
        if (current == targetLayers && SNOW_COMPATIBILITY.isManagedSnow(state)) {
            return false;
        }
        SnowWorldMutation mutation = SNOW_COMPATIBILITY.createLayerMutation(level, pos, state, targetLayers, allowPlace);
        return mutation != null && mutation.apply(level);
    }

    // Queueing variants restored
    protected static boolean queueSnowLayersIfNeeded(ServerLevel level, BlockPos pos, int targetLayers, boolean allowPlace) {
        targetLayers = Mth.clamp(targetLayers, 1, 8);
        BlockState state = level.getBlockState(pos);

        // Enforce per-column cap based on gamerule/maxSnowHeight
        targetLayers = clampLayersForColumnCap(level, pos, state, targetLayers);
        if (targetLayers <= 0) return false;

        int current = SNOW_COMPATIBILITY.getManagedLayers(level, pos, state);
        targetLayers = Mth.clamp(targetLayers, 1, Math.max(1, SNOW_COMPATIBILITY.getMaxManagedLayers(level, pos, state)));
        if (current == targetLayers && SNOW_COMPATIBILITY.isManagedSnow(state)) {
            return false;
        }
        SnowWorldMutation mutation = SNOW_COMPATIBILITY.createLayerMutation(level, pos, state, targetLayers, allowPlace);
        if (mutation == null) {
            return false;
        }
        MUTATION_BATCH.queueMutation(mutation);
        snowPill.add(mutation.trackingPos().immutable());
        return true;
    }

    // Ensure we never exceed configured max snow height (layers) per column
    private static int clampLayersForColumnCap(ServerLevel level, BlockPos pos, BlockState currentState, int targetLayers) {
        int cap = level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        if (cap <= 0) return targetLayers; // no cap

        // compute current total in this vertical snow column
        int currentAtPos = SNOW_COMPATIBILITY.getManagedLayers(level, pos, currentState);
        int total = SnowColumnInspector.computeManagedColumnTotal(level, pos, SNOW_COMPATIBILITY);

        int remaining = cap - (total - currentAtPos); // how many layers we can have at this position after change
        if (remaining <= 0) {
            // No room for more; allow lowering but not raising
            return Math.min(targetLayers, currentAtPos);
        }

        // If we are increasing layers at this pos, clamp to remaining
        if (targetLayers > currentAtPos) {
            int delta = targetLayers - currentAtPos;
            if (delta > remaining) {
                targetLayers = currentAtPos + remaining;
            }
        }
        return Mth.clamp(targetLayers, 1, 8);
    }

    protected static BlockPos findSnowBlockInRadius(ServerLevel level, BlockPos center, int radius) {
        ChunkSource chunkSource = level.getChunkSource();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                int chunkX = (center.getX() + x) >> 4;
                int chunkZ = (center.getZ() + z) >> 4;

                ChunkAccess chunk = chunkSource.getChunk(chunkX, chunkZ, false);
                if (chunk == null) continue;

                for (int y = -5; y <= 5; ++y) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) {
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

    protected static boolean queueClearIfNeeded(ServerLevel level, BlockPos pos, boolean toWater) {
        BlockState state = level.getBlockState(pos);
        SnowWorldMutation mutation = SNOW_COMPATIBILITY.createClearMutation(level, pos, state, toWater);
        if (mutation == null) {
            return false;
        }
        MUTATION_BATCH.queueMutation(mutation);
        return true;
    }

    protected static void queueChange(BlockPos pos, BlockState state, int flags) {
        MUTATION_BATCH.queueChange(pos, state, flags);
    }

    // Record a snow column map change to be applied at batch end
    public static void accumulateColumnUpdate(ServerLevel level, BlockPos pos, BlockState state) {
        MUTATION_BATCH.accumulateColumnUpdate(level, pos, state, SNOW_COMPATIBILITY);
    }

    public static void accumulateColumnUpdate(BlockPos pos, BlockState state) {
        ServerLevel overworld = net.Gabou.gaboulibs.util.WorldContext.getOverworld();
        if (overworld != null) {
            accumulateColumnUpdate(overworld, pos, state);
        }
    }

    protected static int processQueuedChanges(ServerLevel level, int limit) {
        return MUTATION_BATCH.processQueuedChanges(level, limit, SNOW_COMPATIBILITY);
    }

    protected static void finalizeChunkBatch(ServerLevel level) {
        MUTATION_BATCH.finalizeChunkBatch(level);
        snowPill.clear();
        applyCycleTotal = 0;
        applyCycleProcessed = 0;
    }

    protected static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }
    }

    protected static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }

    public static int computeGlobalAvg(ServerLevel level) {
        return HISTORY_QUERY_SERVICE.computeGlobalAvg(level);
    }


    /**
     * Sum minimum layers across all finished storms (exclude active currentStormId if > 0).
     * This baseline should exist at every possible snow column.
     */
    public static int computeGlobalMinSum(ServerLevel level) {
        return HISTORY_QUERY_SERVICE.computeGlobalMinSum(level);
    }

    protected static SnowRecord aggregateFinishedStormSums(ServerLevel level) {
        return HISTORY_QUERY_SERVICE.aggregateFinishedStormSums(level);
    }

    public static void onServerStarting(int config, boolean snowStorm, int snowHeight) {
        tickThresholdSnowReplacer = config;
        snowFeatureEnabled = snowStorm;
        maxHeightForSnow = snowHeight;
        tickCounter = 0;
        playerPositions.clear();
        ChunkQueue.clear();
        MUTATION_BATCH.clear();
        LOAD_RECONCILER.clear();
        snowPill.clear();
        applyCycleTotal = 0;
        applyCycleProcessed = 0;


    }

    public static void onServerStopping() {
        clear();
    }

    public static void onConfigReload(int config, boolean snowStorm, int snowHeight) {
        if(snowHeight != maxHeightForSnow) {
            maxHeightForSnow = snowHeight;
           needUpdateSnowFeature = true;
        }
        tickThresholdSnowReplacer = config;
        snowFeatureEnabled = snowStorm;

    }

    public static void onSeasonChange(ServerLevel level) {
        ChunkQueue.clear();
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
            return temperature < 0.5F ? 3 : 25;
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
}

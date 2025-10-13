package com.Gabou.sereneseasonsplus.features;


import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.features.snowstorm.ISnowStormLevel;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import com.Gabou.sereneseasonsplus.storage.SnowRecord;
import com.Gabou.sereneseasonsplus.tags.SSPTags;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.util.MinecraftServerAccess;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.GameRules;
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
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommonSnowBlockFeature {

    public static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");

    protected static final Map<ServerPlayer, BlockPos> playerPositions = new ConcurrentHashMap<>();

    protected static int tickThresholdSnowReplacer;
    protected static int tickCounter = 0;

    public static ISnowEnvironmentHandler HANDLER = new DefaultSnowEnvironmentHandler();

    protected static final int MAX_ATTEMPTS = 64;

    public record QueuedChange(BlockPos pos, BlockState state, int flags) { }
    static final Map<BlockPos, QueuedChange> pendingChanges = new LinkedHashMap<>();
    static final Set<ChunkPos> chunksToDirty = new HashSet<>();

    // Accumulate per-chunk snow column updates to avoid repeated chunk lookups
    static final Map<ChunkPos, Map<BlockPos, Integer>> pendingColumnMapUpdates = new HashMap<>();
    // Accumulate per-chunk ice adds (rivers/oceans) to thaw efficiently later
    static final Map<ChunkPos, java.util.Set<BlockPos>> pendingIceAdds = new HashMap<>();

    // restored for visibility or metrics during a batch
    static final List<BlockPos> snowPill = new ArrayList<>();

    static int applyCycleTotal = 0;
    static int applyCycleProcessed = 0;

    // Piling speed controls for active storms
    // When true, use immediate/fast piling (current behavior). When false, pile gradually.
    public static boolean FAST_PILING_MODE = false;
    // Target time to reach full storm distribution (in ticks). Default ~8000 per request.
    public static int ACTIVE_STORM_TARGET_TICKS = 8000;
    // Multiplier to scale the speed (1.0 = default; >1 faster, <1 slower)
    public static float STORM_INTENSITY_MULTIPLIER = 1.0f;

    public static void setFastPilingMode(boolean enabled) { FAST_PILING_MODE = enabled; }
    public static void setActiveStormTargetTicks(int ticks) { ACTIVE_STORM_TARGET_TICKS = Math.max(1, ticks); }
    public static void setStormIntensityMultiplier(float mult) { STORM_INTENSITY_MULTIPLIER = Math.max(0.01f, mult); }

    public static int getTickCounter() { return tickCounter; }

    public static void handleServerTick(MinecraftServer server, ServerLevel level) {
        if (level == null || level.isClientSide()) return;

        ++tickCounter;

        if(!snowQueue.isEmpty()){
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

            if (ChunkQueue.isEmpty()) ChunkQueue.shuffle();

            while ((entry = ChunkQueue.poll()) != null) {
                boolean timeUp = (
                        ((MinecraftServerAccess) server).sereneseasonsplus$tempsEcoule() && processed >= 5
                ) || processed >= 20;
                if (timeUp) {
                    if (entry.type() == ChunkQueue.TaskType.APPLY_SNOW) {
                        enqueueChunkForSnowApply(entry.pos(), entry.subSeason());
                    } else {
                        enqueueChunkForSnowMelt(entry.pos(), entry.fullClear());
                    }
                    break;
                }

                boolean changed = false;
                ChunkPos chunkPos = entry.pos();
                if (!level.hasChunk(chunkPos.x, chunkPos.z)) { continue; }

//                if(chunkPos.equals(new ChunkPos(23,-39))){
//                    LOGGER.info("Processing chunk -7,-5 for task {} (fullClear={})", entry.type(), entry.fullClear());
//                }
                LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                if (chunk == null) { continue; }

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
                            chunksToDirty.add(chunkPos);
                            changed = true;
                        }
                    }
                    case MELT_SNOW -> {
                        changed = meltSnowInChunk(level, chunkPos, entry.fullClear());
                        if (changed) {
                            chunksToDirty.add(chunkPos);
                        }
                    }
                }
                processed++;
            }
        }

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
    private static final Queue<LevelChunk> snowQueue = new ConcurrentLinkedQueue<>();

    // On chunk load, only cache surface height; do not enqueue or modify snow lists
    public static void handleOnChunkLoad(LevelChunk chunk) {
        snowQueue.add(chunk);
    }

    private static void chunkHandler(ServerLevel level)
    {
        LevelChunk chunk;
        while ((chunk = snowQueue.poll()) != null) {
            if (!(chunk instanceof ISnowTrackedChunk tracked)) continue;

            boolean needSurface = tracked.sereneseasonsplus$getSurfaceHeight() == -1;
            boolean needAvail = tracked.sereneseasonsplus$getAvailableSnowColumns() == -1;
            if (!needSurface && !needAvail) continue;

            if (needSurface) {
                int centerX = chunk.getPos().getMiddleBlockX();
                int centerZ = chunk.getPos().getMiddleBlockZ();
                int surfaceHeight = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        centerX,
                        centerZ
                );
                tracked.sereneseasonsplus$setSurfaceHeight(surfaceHeight);
            }

            if (needAvail) {
                int baseX = chunk.getPos().getMinBlockX();
                int baseZ = chunk.getPos().getMinBlockZ();
                int count = 0;
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int x = baseX + dx;
                        int z = baseZ + dz;
                        BlockPos surface = findPlacementTop(level, x, z);
                        if (surface != null) count++;
                    }
                }
                tracked.sereneseasonsplus$setAvailableSnowColumns(count);
            }
        }

    }

    public static void enqueueChunkForSnowApply(ChunkPos chunkPos, Season.SubSeason subSeason) {
        ChunkQueue.enqueueApply(chunkPos, subSeason);
    }
    public static void enqueueChunkForSnowMelt(ChunkPos chunkPos, boolean fullClear) {
        ChunkQueue.enqueueMelt(chunkPos, fullClear);
    }

    protected static void passifSnowBlocks(ServerLevel level) {
        for (Map.Entry<ServerPlayer, BlockPos> entry : playerPositions.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();

            int simulationDistance = getSimulationDistance(player);
            int radius = Mth.clamp(simulationDistance * 16, 16, 64);

            int blocksToReplace = HANDLER.getBlocksToReplace(level, playerPos);
            if (blocksToReplace < 0) {
                if (!EnvironmentHelper.isRainning(level, playerPos)) continue;

                final RandomSource random = level.random;
                for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                    int dx = random.nextInt(radius * 2 + 1) - radius;
                    int dz = random.nextInt(radius * 2 + 1) - radius;
                    int x = playerPos.getX() + dx;
                    int z = playerPos.getZ() + dz;

                    BlockPos surface = findPlacementTop(level, x, z);
                    if (surface == null) continue;

                    BlockState st = level.getBlockState(surface);
                    BlockPos targetPos = surface;
                    int targetLayers = 1;

                    if (st.is(Blocks.SNOW)) {
                        int cur = st.getValue(SnowLayerBlock.LAYERS);
                        if (cur < 8) {
                            targetLayers = cur + 1;
                        } else {
                            // stack above
                            BlockPos above = surface.above();
                            if (!level.isEmptyBlock(above)) continue;
                            targetPos = above;
                            targetLayers = 1;
                        }
                    } else if (st.is(Blocks.SNOW_BLOCK)) {
                        BlockPos above = surface.above();
                        if (!level.isEmptyBlock(above)) continue;
                        targetPos = above;
                        targetLayers = 1;
                    } else {
                        targetPos = surface;
                        targetLayers = 1;
                    }

                    if (placeOrQueueLayers(level, targetPos, targetLayers, true, false)) {
                        BlockState ns = level.getBlockState(targetPos);
                        accumulateColumnUpdate(targetPos, ns);
                    }

                    // Try freezing water in water biomes near this attempt
                    BlockPos below = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z).below();
                    tryFreezeWaterAt(level, below);
                }
            } else {
                for (int i = 0; i < blocksToReplace; ++i) {
                    BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                    if (targetPos == null) continue;

                    SnowUtils.breakOrDecrementLayer(level, targetPos);

                    BlockState ns = level.getBlockState(targetPos);
                    accumulateColumnUpdate(targetPos, ns);
                }
            }
        }
    }

    private static boolean syncTrackedColumnsToWorld(ServerLevel level, LevelChunk chunk) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        ChunkPos cp = chunk.getPos();

        // 🚩 if we have pending changes for this chunk, skip sync entirely
        if (pendingColumnMapUpdates.containsKey(cp) ||
                pendingChanges.keySet().stream().anyMatch(p -> (p.getX() >> 4) == cp.x && (p.getZ() >> 4) == cp.z)) {
            return false;
        }

        Map<BlockPos, Integer> columns = tracked.sereneseasonsplus$getSnowColumns();
        if (columns == null) return false;

        // --- Merge in any pending updates for this chunk before checking ---
        Map<BlockPos, Integer> pending = pendingColumnMapUpdates.get(cp);
        if (pending != null && !pending.isEmpty()) {
            columns.putAll(pending); // lightweight, no world edit
        }

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
    

    // Enforce baseline per-column from finished storms; if nothing to do, optionally add bias from active storm
    private static boolean applySnowHistoryPass(ServerLevel level, LevelChunk chunk) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        // Skip this chunk entirely if it already reached the max snow cap from the gamerule
        int cap = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
        if (cap > 0 && isChunkAtOrAboveSnowCap(level, chunk, cap)) {
            return false;
        }

        int baseline = computeGlobalMinSum(level);
        if (baseline <= 0) return false;

        boolean any = false;
        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos topCursor = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                BlockPos surface = findPlacementTop(level, x, z);
                if (surface == null) continue;

                // Count current layers in this vertical snow column
                int current = 0;
                cursor.set(surface.getX(), surface.getY(), surface.getZ());
                BlockState at = level.getBlockState(cursor);
                if (!at.is(Blocks.SNOW) && !at.is(Blocks.SNOW_BLOCK)) cursor.move(0, -1, 0);
                while (cursor.getY() >= minY) {
                    BlockState s = level.getBlockState(cursor);
                    if (s.is(Blocks.SNOW)) {
                        current += s.getValue(BlockStateProperties.LAYERS);
                    } else if (s.is(Blocks.SNOW_BLOCK)) {
                        current += 8;
                    } else {
                        break;
                    }
                    cursor.move(0, -1, 0);
                }

                int need = baseline - current;
                // Do not exceed gamerule cap
                if (cap > 0) {
                    int remaining = cap - current;
                    if (remaining <= 0) continue;
                    if (need > remaining) need = remaining;
                }
                if (need <= 0) continue;

                // Place above the existing stack
                topCursor.set(surface.getX(), surface.getY(), surface.getZ());
                while (true) {
                    BlockState s = level.getBlockState(topCursor);
                    if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK)) {
                        topCursor.move(0, 1, 0);
                        continue;
                    }
                    break;
                }

                int remaining = need;
                BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos(topCursor.getX(), topCursor.getY(), topCursor.getZ());
                BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos(topCursor.getX(), topCursor.getY() - 1, topCursor.getZ());

                if (belowPos.getY() >= minY) {
                    BlockState topState = level.getBlockState(belowPos);
                    if (topState.is(Blocks.SNOW) || topState.is(Blocks.SNOW_BLOCK)) {
                        int currentLayers = topState.is(Blocks.SNOW_BLOCK)
                                ? 8
                                : topState.getValue(BlockStateProperties.LAYERS);
                        int freeSpace = 8 - currentLayers;
                        if (freeSpace > 0 && remaining > 0) {
                            int add = Math.min(freeSpace, remaining);
                            int targetLayers = currentLayers + add;
                            if (placeOrQueueLayers(level, belowPos, targetLayers, true, true)) {
                                tracked.sereneseasonsplus$getSnowColumns().put(belowPos.immutable(), targetLayers);
                                any = true;
                            }
                            remaining -= add;
                        }
                        if (remaining <= 0) {
                            continue;
                        }
                    }
                }

                while (remaining > 0 && placePos.getY() < level.getMaxBuildHeight()) {
                    int toPlace = Math.min(8, remaining);
                    if (placeOrQueueLayers(level, placePos, toPlace, true, true)) {
                        tracked.sereneseasonsplus$getSnowColumns().put(placePos.immutable(), toPlace);
                        any = true;
                    }
                    remaining -= toPlace;
                    placePos.move(0, 1, 0);
                }
            }
        }

        // After baseline, add distribution either from active storm or combined finished storms
        SnowHistorySavedData sd = SnowHistorySavedData.get(level);
        if (sd != null) {
            if (sd.currentStormId > 0) {
                SnowRecord rec = sd.snowHistory.get(sd.currentStormId);
                if (rec != null) {
                    return applySnowPattern(level, chunk, rec, level.random) || any;
                }
            } else {
                SnowRecord combined = aggregateFinishedStormSums(level);
                if (combined != null) {
                    return applyCombinedFinishedPattern(level, chunk, combined, level.random) || any;
                }
            }
        }
        return any;
    }

    // Attempts to freeze a water block at pos if conditions are met. Returns true if a block changed.
    public static boolean tryFreezeWaterAt(ServerLevel level, BlockPos pos) {
        if (pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER)) return false;

        BlockPos sample = pos.above();
        if (!EnvironmentHelper.isRainning(level, sample)) return false;
        // Do not freeze under roofs/caves
        if (!isExposedToSky(level, sample)) return false;
        if (!HANDLER.isColdEnoughForSnow(level, sample)) return false;
        if (!isWaterBiome(level, pos)) return false;

        BlockState ice = Blocks.ICE.defaultBlockState();
        if (level.setBlockAndUpdate(pos, ice)) {
            accumulateColumnUpdate(pos, ice);
            return true;
        }
        return false;
    }

    private static boolean isWaterBiome(ServerLevel level, BlockPos pos) {
        try {
            Holder<net.minecraft.world.level.biome.Biome> holder = level.getBiome(pos);
            return holder.unwrapKey()
                    .map(key -> {
                        ResourceLocation rl = key.location();
                        String path = rl.getPath();
                        return path.contains("ocean") || path.contains("river");
                    })
                    .orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean applySnowPatternFromActiveRecord(ServerLevel level, LevelChunk chunk) {
        SnowHistorySavedData sd =
                SnowHistorySavedData.get(level);

        if (sd != null && sd.currentStormId > 0) {
            SnowRecord rec = sd.snowHistory.get(sd.currentStormId);
            if (rec != null) {
                return applySnowPattern(level, chunk, rec, level.random);
            }
        }
        return false;
    }

    private static boolean applySnowPattern(ServerLevel level,
                                            LevelChunk chunk,
                                            SnowRecord record,
                                            RandomSource rng) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        // Skip this chunk entirely if it already reached the max snow cap from the gamerule
        int cap = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
        if (cap > 0 && isChunkAtOrAboveSnowCap(level, chunk, cap)) {
            return false;
        }

        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();

        float avg = Math.max(0f, record.avgLayers);
        float coverage = Mth.clamp(avg / 8f, 0.10f, 0.85f); // still normalized to 8, but can exceed in layers

        boolean any = false;

        // Compute per-chunk storm progress (0..1). If fast mode, jump to 1.
        float progress = 1.0f;
        int currentTick = getTickCounter();
        if (!FAST_PILING_MODE) {
            com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData sd = com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData.get(level);
            int activeId = (sd != null) ? sd.currentStormId : 0;
            if (activeId > 0) {
                if (tracked.sereneseasonsplus$getStormIdApplied() != activeId) {
                    // New storm: reset progress for this chunk
                    tracked.sereneseasonsplus$setStormIdApplied(activeId);
                    tracked.sereneseasonsplus$setStormProgress(0f);
                    tracked.sereneseasonsplus$setLastProgressTick(currentTick);
                }
                float cur = tracked.sereneseasonsplus$getStormProgress();
                int last = tracked.sereneseasonsplus$getLastProgressTick();
                int dt = Math.max(1, currentTick - last);
                float delta = (float) dt / (float) Math.max(1, ACTIVE_STORM_TARGET_TICKS);
                cur = Mth.clamp(cur + (delta * STORM_INTENSITY_MULTIPLIER), 0f, 1f);
                tracked.sereneseasonsplus$setStormProgress(cur);
                tracked.sereneseasonsplus$setLastProgressTick(currentTick);
                progress = cur;
            }
        }

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                float white = rng.nextFloat();
                double wave = Math.sin((x * 0.12D) + (z * 0.12D));
                float noise = Mth.clamp((float) ((white * 0.7) + ((wave + 1.0) * 0.15)), 0f, 1f);

                if (noise > coverage) continue;

                BlockPos surface = findPlacementTop(level, x, z);
                if (surface == null) continue;

                // layers from record
                int minL = Math.max(1, Math.round(record.minLayers));
                int maxL = Math.max(minL, Math.round(record.maxLayers));
                int span = Math.max(1, maxL - minL + 1);
                int pick = minL + rng.nextInt(span);

                int towardAvg = Math.round(Mth.lerp(0.35f, pick, avg));
                int totalLayers = Math.max(1, towardAvg); // can exceed 8 now

                // Desired layers for current progress (0..1)
                int desired = FAST_PILING_MODE ? totalLayers : Math.max(0, Math.round(totalLayers * progress));
                if (desired <= 0) continue;

                // --- place stacked snow ---
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(surface.getX(), surface.getY(), surface.getZ());
                // Count current total layers in this column (downwards)
                int minY = level.getMinBuildHeight();
                int currentTotal = 0;
                BlockPos.MutableBlockPos down = new BlockPos.MutableBlockPos(surface.getX(), surface.getY(), surface.getZ());
                // If current pos is not snow, step down one to find stack
                BlockState at = level.getBlockState(down);
                if (!at.is(Blocks.SNOW) && !at.is(Blocks.SNOW_BLOCK)) {
                    down.move(0, -1, 0);
                }
                while (down.getY() >= minY) {
                    BlockState s = level.getBlockState(down);
                    if (s.is(Blocks.SNOW)) {
                        currentTotal += s.getValue(BlockStateProperties.LAYERS);
                    } else if (s.is(Blocks.SNOW_BLOCK)) {
                        currentTotal += 8;
                    } else {
                        break;
                    }
                    down.move(0, -1, 0);
                }

                int need = desired - currentTotal;
                if (cap > 0) {
                    int remaining = cap - currentTotal;
                    if (remaining <= 0) continue;
                    if (need > remaining) need = remaining;
                }
                if (need <= 0) continue;

                // Place the needed increment at/above the current top
                BlockState existing = level.getBlockState(cursor);
                if (existing.is(Blocks.SNOW) || existing.is(Blocks.SNOW_BLOCK)) {
                    int currentLayers = existing.is(Blocks.SNOW_BLOCK) ? 8 : existing.getValue(BlockStateProperties.LAYERS);
                    int freeSpace = 8 - currentLayers;
                    if (freeSpace > 0 && need > 0) {
                        int add = Math.min(freeSpace, need);
                        int targetLayers = currentLayers + add;
                        if (placeOrQueueLayers(level, cursor, targetLayers, true, true)) {
                            tracked.sereneseasonsplus$getSnowColumns().put(cursor.immutable(), targetLayers);
                            any = true;
                        }
                        need -= add;
                    }
                    if (need > 0) {
                        cursor.move(0, 1, 0);
                    }
                }

                while (need >= 8 && cursor.getY() < level.getMaxBuildHeight()) {
                    if (placeOrQueueLayers(level, cursor, 8, true, true)) {
                        tracked.sereneseasonsplus$getSnowColumns().put(cursor.immutable(), 8);
                        any = true;
                    }
                    need -= 8;
                    cursor.move(0, 1, 0);
                }

                if (need > 0 && cursor.getY() < level.getMaxBuildHeight()) {
                    if (placeOrQueueLayers(level, cursor, need, true, true)) {
                        tracked.sereneseasonsplus$getSnowColumns().put(cursor.immutable(), need);
                        any = true;
                    }
                }
            }
        }

        return any;
    }

    // Combined pattern from finished storms: also respect cap
    private static boolean applyCombinedFinishedPattern(ServerLevel level,
                                                        LevelChunk chunk,
                                                        SnowRecord combined,
                                                        RandomSource rng) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return false;

        int cap = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
        if (cap > 0 && isChunkAtOrAboveSnowCap(level, chunk, cap)) {
            return false;
        }

        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();

        int minL = Math.max(0, Math.round(combined.minLayers));
        int avgL = Math.max(minL, Math.round(combined.avgLayers));
        int maxL = Math.max(avgL, Math.round(combined.maxLayers));

        boolean any = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                float white = rng.nextFloat();
                double wave = Math.sin((x * 0.12D) + (z * 0.12D));
                float noise = Mth.clamp((float) ((white * 0.7) + ((wave + 1.0) * 0.15)), 0f, 1f);

                int pick = minL + Math.round(noise * Math.max(0, maxL - minL));
                int towardAvg = Math.round(Mth.lerp(0.35f, pick, avgL));
                int desired = Math.max(minL, towardAvg);
                if (desired <= 0) continue;

                BlockPos surface = findPlacementTop(level, x, z);
                if (surface == null) continue;

                // Compute current total in this column
                int minY = level.getMinBuildHeight();
                int currentTotal = 0;
                BlockPos.MutableBlockPos down = new BlockPos.MutableBlockPos(surface.getX(), surface.getY(), surface.getZ());
                BlockState at = level.getBlockState(down);
                if (!at.is(Blocks.SNOW) && !at.is(Blocks.SNOW_BLOCK)) {
                    down.move(0, -1, 0);
                }
                while (down.getY() >= minY) {
                    BlockState s = level.getBlockState(down);
                    if (s.is(Blocks.SNOW)) {
                        currentTotal += s.getValue(BlockStateProperties.LAYERS);
                    } else if (s.is(Blocks.SNOW_BLOCK)) {
                        currentTotal += 8;
                    } else {
                        break;
                    }
                    down.move(0, -1, 0);
                }

                int need = desired - currentTotal;
                if (cap > 0) {
                    int remaining = cap - currentTotal;
                    if (remaining <= 0) continue;
                    if (need > remaining) need = remaining;
                }
                if (need <= 0) continue;

                // Place increments at/above the top
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(surface.getX(), surface.getY(), surface.getZ());
                BlockState existing = level.getBlockState(cursor);
                if (existing.is(Blocks.SNOW) || existing.is(Blocks.SNOW_BLOCK)) {
                    int currentLayers = existing.is(Blocks.SNOW_BLOCK) ? 8 : existing.getValue(BlockStateProperties.LAYERS);
                    int freeSpace = 8 - currentLayers;
                    if (freeSpace > 0 && need > 0) {
                        int add = Math.min(freeSpace, need);
                        int targetLayers = currentLayers + add;
                        if (placeOrQueueLayers(level, cursor, targetLayers, true, true)) {
                            tracked.sereneseasonsplus$getSnowColumns().put(cursor.immutable(), targetLayers);
                            any = true;
                        }
                        need -= add;
                    }
                    if (need > 0) cursor.move(0, 1, 0);
                }

                while (need >= 8 && cursor.getY() < level.getMaxBuildHeight()) {
                    if (placeOrQueueLayers(level, cursor, 8, true, true)) {
                        tracked.sereneseasonsplus$getSnowColumns().put(cursor.immutable(), 8);
                        any = true;
                    }
                    need -= 8;
                    cursor.move(0, 1, 0);
                }

                if (need > 0 && cursor.getY() < level.getMaxBuildHeight()) {
                    if (placeOrQueueLayers(level, cursor, need, true, true)) {
                        tracked.sereneseasonsplus$getSnowColumns().put(cursor.immutable(), need);
                        any = true;
                    }
                }
            }
        }

        return any;
    }

    // Checks whether any snow column inside the chunk has total layers >= cap
    private static boolean isChunkAtOrAboveSnowCap(ServerLevel level, LevelChunk chunk, int capLayers) {
        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();

        BlockPos.MutableBlockPos down = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;

                BlockPos surface = findPlacementTop(level, x, z);
                if (surface == null) continue;

                int total = 0;
                down.set(surface.getX(), surface.getY(), surface.getZ());
                BlockState at = level.getBlockState(down);
                if (!at.is(Blocks.SNOW) && !at.is(Blocks.SNOW_BLOCK)) down.move(0, -1, 0);
                while (down.getY() >= minY) {
                    BlockState s = level.getBlockState(down);
                    if (s.is(Blocks.SNOW)) total += s.getValue(BlockStateProperties.LAYERS);
                    else if (s.is(Blocks.SNOW_BLOCK)) total += 8;
                    else break;
                    if (total >= capLayers) return true;
                    down.move(0, -1, 0);
                }
            }
        }
        return false;
    }


    public static boolean meltSnowInChunk(ServerLevel level, ChunkPos chunkPos, boolean fullClear) {
        LevelChunk chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
        if (!(chunk instanceof ISnowTrackedChunk tracked)) {
            return false;
        }

        Map<BlockPos, Integer> columns = tracked.sereneseasonsplus$getSnowColumns();
        if (columns == null || columns.isEmpty()) {
            // Still try to thaw ice even if no snow is tracked
            return meltTrackedIce(level, tracked);
        }

        boolean changed = false;

        if (fullClear) {
            for (BlockPos pos : new ArrayList<>(columns.keySet())) {
                changed |= queueClearIfNeeded(level, pos, false);
                columns.remove(pos);
            }
            return changed;
        }

        Map<Long, BlockPos> topByColumn = new HashMap<>();
        for (BlockPos p : columns.keySet()) {
            long key = (((long) p.getX()) << 32) ^ (p.getZ() & 0xffffffffL);
            BlockPos cur = topByColumn.get(key);
            if (cur == null || p.getY() > cur.getY()) {
                topByColumn.put(key, p.immutable());
            }
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (BlockPos top : topByColumn.values()) {
            BlockState state = level.getBlockState(top);

            if (state.is(Blocks.SNOW_BLOCK)) {
                changed |= queueSnowLayersIfNeeded(level, top, 7, false);
                columns.put(top.immutable(), 7);
            } else if (state.is(Blocks.SNOW)) {
                int layers = state.getValue(BlockStateProperties.LAYERS);
                if (layers <= 1) {
                    changed |= queueClearIfNeeded(level, top, false);
                    columns.remove(top);
                } else {
                    changed |= queueSnowLayersIfNeeded(level, top, layers - 1, false);
                    columns.put(top.immutable(), layers - 1);
                }
            } else {
                int minY = level.getMinBuildHeight();
                cursor.set(top.getX(), top.getY(), top.getZ());
                while (cursor.getY() >= minY) {
                    BlockState s = level.getBlockState(cursor);
                    if (s.is(Blocks.SNOW_BLOCK)) {
                        changed |= queueSnowLayersIfNeeded(level, cursor.immutable(), 7, false);
                        columns.put(cursor.immutable(), 7);
                        break;
                    } else if (s.is(Blocks.SNOW)) {
                        int l = s.getValue(BlockStateProperties.LAYERS);
                        if (l <= 1) {
                            changed |= queueClearIfNeeded(level, cursor.immutable(), false);
                            columns.remove(cursor);
                        } else {
                            changed |= queueSnowLayersIfNeeded(level, cursor.immutable(), l - 1, false);
                            columns.put(cursor.immutable(), l - 1);
                        }
                        break;
                    }
                    cursor.move(0, -1, 0);
                }
            }
            // Do not clear the entire column below in a regular melt pass.
            // Each melt tick only reduces the topmost stack by 1 layer.
        }

        // Also thaw tracked ice to water
        changed |= meltTrackedIce(level, tracked);

        return changed;
    }

    private static boolean meltTrackedIce(ServerLevel level, ISnowTrackedChunk tracked) {
        boolean changed = false;
        java.util.Set<BlockPos> copy = new java.util.HashSet<>(tracked.sereneseasonsplus$getIceColumns());
        for (BlockPos p : copy) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.ICE)) {
                queueChange(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                tracked.sereneseasonsplus$getIceColumns().remove(p);
                changed = true;
            } else {
                // cleanup stale
                tracked.sereneseasonsplus$getIceColumns().remove(p);
            }
        }
        return changed;
    }

    // surface selection
    private static BlockPos findPlacementTop(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos base = new BlockPos(x, y, z);
        BlockState at = level.getBlockState(base);

        BlockState snow = Blocks.SNOW.defaultBlockState();
        if ((level.isEmptyBlock(base) || at.canBeReplaced()) && snow.canSurvive(level, base) && isExposedToSky(level, base)) {
            return base;
        }

        BlockPos up = base.above();
        if (snow.canSurvive(level, up) && level.isEmptyBlock(up) && isExposedToSky(level, up)) {
            return up;
        }

        if (at.is(Blocks.SNOW) && isExposedToSky(level, base)) return base;

        return null;
    }

    private static boolean isExposedToSky(ServerLevel level, BlockPos pos) {
        if(pos.equals(new BlockPos(-98,63,-104)))
        {
            int debug = 0;
        }
        try {
            return level.canSeeSkyFromBelowWater(pos);
        } catch (Throwable t) {
            return true; // fail open to avoid breaking placement if method differs
        }
    }

    /**
     * Unified setter
     * If queue is true we enqueue using the classic queueSnowLayersIfNeeded
     * If queue is false we set immediately
     */
    private static boolean placeOrQueueLayers(ServerLevel level, BlockPos pos, int targetLayers, boolean allowPlace, boolean queue) {
        if (queue) {
            return queueSnowLayersIfNeeded(level, pos, targetLayers, allowPlace);
        }

        targetLayers = Mth.clamp(targetLayers, 1, 8);
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            if (allowPlace && !isExposedToSky(level, pos)) return false;
            int current = state.getValue(SnowLayerBlock.LAYERS);
            if (current == targetLayers) return false;
            BlockState newState = state.setValue(SnowLayerBlock.LAYERS, targetLayers);
            return level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        } else if (state.is(Blocks.SNOW_BLOCK)) {
            if (allowPlace && !isExposedToSky(level, pos)) return false;
            if (targetLayers == 8) return false;
            BlockState newState = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, targetLayers);
            return level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        } else if (allowPlace && (level.isEmptyBlock(pos) || state.canBeReplaced())) {
            if (!isExposedToSky(level, pos)) return false;
            BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, targetLayers);
            if (!snow.canSurvive(level, pos)) return false;
            return level.setBlock(pos, snow, Block.UPDATE_CLIENTS);
        }
        return false;
    }

    // Queueing variants restored
    private static boolean queueSnowLayersIfNeeded(ServerLevel level, BlockPos pos, int targetLayers, boolean allowPlace) {
        targetLayers = Mth.clamp(targetLayers, 1, 8);
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            if (allowPlace && !isExposedToSky(level, pos)) return false;
            int current = state.getValue(SnowLayerBlock.LAYERS);
            if (current == targetLayers) return false;
            BlockState newState = state.setValue(SnowLayerBlock.LAYERS, targetLayers);
            queueChange(pos, newState, Block.UPDATE_CLIENTS);
            snowPill.add(pos.immutable());
            return true;
        } else if (state.is(Blocks.SNOW_BLOCK)) {
            if (allowPlace && !isExposedToSky(level, pos)) return false;
            if (targetLayers == 8) return false;
            BlockState newState = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, targetLayers);
            queueChange(pos, newState, Block.UPDATE_CLIENTS);
            snowPill.add(pos.immutable());
            return true;
        } else if (allowPlace && (level.isEmptyBlock(pos) || state.canBeReplaced())) {
            if (!isExposedToSky(level, pos)) return false;
            BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, targetLayers);
            if (!snow.canSurvive(level, pos)) return false;
            queueChange(pos, snow, Block.UPDATE_CLIENTS);
            snowPill.add(pos.immutable());
            return true;
        }
        return false;
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

    private static boolean queueClearIfNeeded(ServerLevel level, BlockPos pos, boolean toWater) {
        BlockState wanted = toWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockState current = level.getBlockState(pos);
        if (current.is(wanted.getBlock())) return false;
        queueChange(pos, wanted, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        return true;
    }

    private static void queueChange(BlockPos pos, BlockState state, int flags) {
        BlockPos imm = pos.immutable();
        pendingChanges.put(imm, new QueuedChange(imm, state, flags));
    }

    // Record a snow column map change to be applied at batch end
    public static void accumulateColumnUpdate(BlockPos pos, BlockState state) {
        ChunkPos cp = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        if (state.is(Blocks.ICE)) {
            pendingIceAdds.computeIfAbsent(cp, k -> new java.util.HashSet<>()).add(pos.immutable());
            chunksToDirty.add(cp);
            return;
        }
        int val;
        if (state.is(Blocks.SNOW) && state.hasProperty(SnowLayerBlock.LAYERS)) {
            val = state.getValue(SnowLayerBlock.LAYERS);
        } else if (state.is(Blocks.SNOW_BLOCK)) {
            val = 8;
        } else {
            val = 0; // signal removal
        }
        pendingColumnMapUpdates
                .computeIfAbsent(cp, k -> new HashMap<>())
                .put(pos.immutable(), val);
        // ensure the chunk is marked dirty at the end of the batch
        chunksToDirty.add(cp);
    }

    private static int processQueuedChanges(ServerLevel level, int limit) {
        if (pendingChanges.isEmpty()) return 0;
        int applied = 0;
        Iterator<Map.Entry<BlockPos, QueuedChange>> it = pendingChanges.entrySet().iterator();
        while (it.hasNext() && applied < limit) {
            Map.Entry<BlockPos, QueuedChange> e = it.next();
            QueuedChange qc = e.getValue();
            // Apply the change to the world
            boolean changed = level.setBlock(qc.pos(), qc.state(), qc.flags());

            // Accumulate column map updates to be flushed once per chunk in finalizeChunkBatch
            if (changed) {
                accumulateColumnUpdate(qc.pos(), qc.state());
            }
            it.remove();
            applied++;
        }
        return applied;
    }

    private static void finalizeChunkBatch(ServerLevel level) {
        // Flush accumulated snow column updates per chunk
        if (!pendingColumnMapUpdates.isEmpty()) {
            for (Map.Entry<ChunkPos, Map<BlockPos, Integer>> entry : pendingColumnMapUpdates.entrySet()) {
                ChunkPos cp = entry.getKey();
                Map<BlockPos, Integer> updates = entry.getValue();
                if (!level.hasChunk(cp.x, cp.z)) continue;
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
                if (!(chunk instanceof ISnowTrackedChunk tracked)) continue;

                Map<BlockPos, Integer> columns = tracked.sereneseasonsplus$getSnowColumns();
                for (Map.Entry<BlockPos, Integer> up : updates.entrySet()) {
                    int val = up.getValue();
                    if (val <= 0) {
                        columns.remove(up.getKey());
                    } else {
                        // Ensure per-position layers are bounded to [1..8]
                        columns.put(up.getKey().immutable(), Mth.clamp(val, 1, 8));
                    }
                }
            }
            pendingColumnMapUpdates.clear();
        }

        // Flush accumulated ice adds per chunk (track where we froze water)
        if (!pendingIceAdds.isEmpty()) {
            for (Map.Entry<ChunkPos, java.util.Set<BlockPos>> e : pendingIceAdds.entrySet()) {
                ChunkPos cp = e.getKey();
                if (!level.hasChunk(cp.x, cp.z)) continue;
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
                if (!(chunk instanceof ISnowTrackedChunk tracked)) continue;
                java.util.Set<BlockPos> set = tracked.sereneseasonsplus$getIceColumns();
                for (BlockPos p : e.getValue()) {
                    if (level.getBlockState(p).is(Blocks.ICE)) set.add(p.immutable());
                }
            }
            pendingIceAdds.clear();
        }

        for (ChunkPos cp : chunksToDirty) {
            if (!level.hasChunk(cp.x, cp.z)) continue;
            LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
            if (chunk == null) continue;
            chunk.setUnsaved(true);
        }
        chunksToDirty.clear();
        pendingChanges.clear();
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
        MinecraftServer server = player.getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }

    public static int computeGlobalAvg(ServerLevel level) {
        SnowHistorySavedData sd = SnowHistorySavedData.get(level);
        if (sd == null || sd.snowHistory.isEmpty()) return 0;

        int excludeId = sd.currentStormId;
        float total = 0f;

        for (Map.Entry<Integer, SnowRecord> e : sd.snowHistory.entrySet()) {
            if (excludeId > 0 && e.getKey() == excludeId) continue;
            SnowRecord rec = e.getValue();

            // Add the average contribution of each completed storm
            total += Math.max(0f, rec.avgLayers);
        }

        // Return the total accumulated snow layers (not clamped to block limits)
        return Math.round(total);
    }


    /**
     * Sum minimum layers across all finished storms (exclude active currentStormId if > 0).
     * This baseline should exist at every possible snow column.
     */
    public static int computeGlobalMinSum(ServerLevel level) {
        SnowHistorySavedData sd = SnowHistorySavedData.get(level);
        if (sd == null || sd.snowHistory.isEmpty()) return 0;

        int excludeId = sd.currentStormId;
        float sumMin = 0f;
        for (Map.Entry<Integer, SnowRecord> e : sd.snowHistory.entrySet()) {
            if (excludeId > 0 && e.getKey() == excludeId) continue;
            SnowRecord rec = e.getValue();
            sumMin += Math.max(0f, rec.minLayers);
        }
        return Math.max(0, Math.round(sumMin));
    }


    private static SnowRecord aggregateFinishedStormMinMax(ServerLevel level) {
        SnowHistorySavedData sd =
                SnowHistorySavedData.get(level);
        if (sd == null || sd.snowHistory.isEmpty()) return null;

        int excludeId = sd.currentStormId;
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, avg = 0f;
        int count = 0;

        for (Map.Entry<Integer, SnowRecord> e : sd.snowHistory.entrySet()) {
            if (excludeId > 0 && e.getKey() == excludeId) continue;
            SnowRecord r = e.getValue();
            min = Math.min(min, r.minLayers);
            max = Math.max(max, r.maxLayers);
            avg += r.avgLayers;
            count++;
        }
        if (count == 0) return null;
        return new SnowRecord(min, avg / count, max, null);
    }

    private static SnowRecord aggregateFinishedStormSums(ServerLevel level) {
        SnowHistorySavedData sd = SnowHistorySavedData.get(level);
        if (sd == null || sd.snowHistory.isEmpty()) return null;
        int excludeId = sd.currentStormId;
        float sumMin = 0f, sumAvg = 0f, sumMax = 0f;
        int count = 0;
        for (Map.Entry<Integer, SnowRecord> e : sd.snowHistory.entrySet()) {
            if (excludeId > 0 && e.getKey() == excludeId) continue;
            SnowRecord r = e.getValue();
            sumMin += Math.max(0f, r.minLayers);
            sumAvg += Math.max(0f, r.avgLayers);
            sumMax += Math.max(0f, r.maxLayers);
            count++;
        }
        if (count == 0) return null;
        return new SnowRecord(sumMin, sumAvg, sumMax, null);
    }

    public static void onServerStarting(int config, boolean snowStorm) {
        tickThresholdSnowReplacer = config;
        tickCounter = 0;
        playerPositions.clear();
        ChunkQueue.clear();
        pendingColumnMapUpdates.clear();
        pendingChanges.clear();
        pendingIceAdds.clear();
        chunksToDirty.clear();
        snowPill.clear();
        applyCycleTotal = 0;
        applyCycleProcessed = 0;


    }

    public static void onServerStopping() {
        playerPositions.clear();
        tickCounter = 0;
        ChunkQueue.clear();
        pendingChanges.clear();
        chunksToDirty.clear();
        pendingColumnMapUpdates.clear();
        snowPill.clear();
        applyCycleTotal = 0;
        applyCycleProcessed = 0;
        pendingIceAdds.clear();

    }

    public static void onConfigReload(int config, boolean snowStorm) {
        tickThresholdSnowReplacer = config;
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

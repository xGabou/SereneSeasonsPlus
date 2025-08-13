package com.Gabou.sereneseasonsextended.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds valid snow placement positions with early biome rejection,
 * per-player throttling (movement-aware), and low-GC scanning.
 */
public final class SnowPlacementSelector {

    /** How far the player must move to ignore throttle (blocks). */
    private static final int MOVEMENT_RESET_RADIUS = 10;
    /** How long to throttle after a failed scan (ticks). 10s = 200 ticks. */
    private static final int THROTTLE_TICKS = 200;
    /** Attempts per call. Tune as desired. */
    private static final int MAX_ATTEMPTS = 12;

    private static final Map<UUID, ThrottleState> THROTTLE = new ConcurrentHashMap<>();

    private SnowPlacementSelector() {}

    /**
     * Try to find a valid position to place snow near {@code center}.
     *
     * @param level  the server level
     * @param player the player driving this attempt (used for throttle)
     * @param center the center position (typically player's block pos)
     * @param radius search radius in blocks
     * @return a valid position to place snow, or null if none / throttled
     */
    public static BlockPos find(ServerLevel level, ServerPlayer player, BlockPos center, int radius) {
        final long now = level.getGameTime();
        final UUID pid = player.getUUID();

        // Throttle: skip if we recently failed and player hasn't moved much.
        final ThrottleState ts = THROTTLE.computeIfAbsent(pid, k -> new ThrottleState());
        if (ts.shouldSkip(now, center, MOVEMENT_RESET_RADIUS)) {
            return null;
        }

        // Early reject: if the center biome isn't cold enough right now, bail.
        // This avoids 12+ random samples in obviously warm biomes.
        if (!level.getBiome(center).value().coldEnoughToSnow(center)) {
            ts.recordFail(now, center);
            return null;
        }

        final RandomSource random = level.random; // cheaper than new Random()
        final MutableBlockPos pos = new MutableBlockPos();
        final MutableBlockPos below = new MutableBlockPos();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            final int dx = random.nextInt(radius * 2 + 1) - radius;
            final int dz = random.nextInt(radius * 2 + 1) - radius;
            final int x = center.getX() + dx;
            final int z = center.getZ() + dz;
            final int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            pos.set(x, y, z);
            below.set(x, y - 1, z);

            // Fast reject: don't build on water.
            if (level.getBlockState(below).is(Blocks.WATER)) {
                continue;
            }

            // Must be empty where snow would go.
            if (!level.isEmptyBlock(pos)) {
                continue;
            }

            // Final correctness: coldEnoughToSnow at the actual pos (includes height).
            if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
                ts.recordSuccess(center); // clear fail window / remember last center
                return pos.immutable();
            }
        }

        // No luck this pass; throttle next attempts unless the player moves.
        ts.recordFail(now, center);
        return null;
    }

    private static final class ThrottleState {
        private long nextAllowedTick = 0L;
        private BlockPos lastCenter = BlockPos.ZERO;

        boolean shouldSkip(long now, BlockPos currentCenter, int radius) {
            if (hasMovedEnough(currentCenter, radius)) {
                // movement resets throttle window
                nextAllowedTick = 0L;
                return false;
            }
            return now < nextAllowedTick;
        }

        void recordFail(long now, BlockPos center) {
            this.lastCenter = center;
            this.nextAllowedTick = now + THROTTLE_TICKS;
        }

        void recordSuccess(BlockPos center) {
            this.lastCenter = center;
            this.nextAllowedTick = 0L;
        }

        private boolean hasMovedEnough(BlockPos current, int radius) {
            if (lastCenter == BlockPos.ZERO) return true;
            final int dx = current.getX() - lastCenter.getX();
            final int dy = current.getY() - lastCenter.getY();
            final int dz = current.getZ() - lastCenter.getZ();
            // Cheaper than sqrt: compare squared distance.
            return (dx * dx + dy * dy + dz * dz) > (radius * radius);
        }
    }
}

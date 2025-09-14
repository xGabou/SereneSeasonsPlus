package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.SereneService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommonSnowPiller {

    protected static int tickThresholdSnowPiller;

    /**
     * Radius in blocks the player must move to reset throttle.
     */
    protected static final int MOVEMENT_RESET_RADIUS = 10;
    /**
     * 10 seconds at 20 tps.
     */
    protected static final int THROTTLE_TICKS = 200;
    /**
     * Sampling attempts per call. Tune as desired.
     */
    protected static final int MAX_ATTEMPTS = 12;

    protected static int tickCounter = 0;

    protected static final Map<UUID, ThrottleState> THROTTLE = new ConcurrentHashMap<>();
    protected static void handleServerTick(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            ++tickCounter;
            if (tickCounter % tickThresholdSnowPiller == 0) {
                SereneService.runAsync(() -> {
                    for (ServerPlayer player : serverLevel.players()) {

                        tickSnow(serverLevel, player, player.blockPosition(), 16);
                    }
                });
            }

        }
    }
    /**
     * Determines attempt count based on weather or snowstorm intensity, then
     * samples positions and places snow layers around the given center.
     *
     * @param level  server level
     * @param player the player (used to throttle repeated failures)
     * @param center search center (often player's block pos)
     * @param radius search radius
     */
    public static void tickSnow(ServerLevel level, ServerPlayer player, BlockPos center, int radius) {
        int attempts;
        if (SereneSeasonsPlus.isProjectAtmosphereLoaded) {
            if (!SereneExtendedConfig.SNOWSTORM_ENABLED.get()) return;


            attempts = Math.max(0, SereneExtendedConfig.SNOWSTORM_INTENSITY.get() / 20);

        } else {
            if (!level.isRaining()) return;


            final boolean coldEnough = level.getBiome(center).value().coldEnoughToSnow(center);
            if (!coldEnough) return;

            final var rnd = level.random;
            attempts = rnd.nextInt(2) + (level.isThundering() ? rnd.nextInt(2) : 0);
        }
        if (attempts <= 0) return;
        for (int i = 0; i < attempts; i++) {
            BlockPos target = findTarget(level, player, center, radius);
            if (target != null) {
                placeSnowAt(level, target);
            }
        }


    }

    /**
     * Finds a valid snow placement position near center, honoring throttle.
     * Returns {@code null} if throttled or if no position is suitable.
     */
    public static @Nullable BlockPos findTarget(ServerLevel level, ServerPlayer player, BlockPos center, int radius) {
        final long now = level.getGameTime();
        final UUID pid = player.getUUID();

        final ThrottleState ts = THROTTLE.computeIfAbsent(pid, k -> new ThrottleState());
        if (ts.shouldSkip(now, center, MOVEMENT_RESET_RADIUS)) {
            return null;
        }


        if (!level.getBiome(center).value().coldEnoughToSnow(center)) {
            ts.recordFail(now, center);
            return null;
        }

        final RandomSource random = level.random;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            final int dx = random.nextInt(radius * 2 + 1) - radius;
            final int dz = random.nextInt(radius * 2 + 1) - radius;
            final int x = center.getX() + dx;
            final int z = center.getZ() + dz;
            final int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            pos.set(x, y, z);
            below.set(x, y - 1, z);


            final BlockState stateAt = level.getBlockState(pos);
            final boolean canStackSnow =
                    stateAt.is(Blocks.SNOW)
                            && stateAt.hasProperty(SnowLayerBlock.LAYERS)
                            && stateAt.getValue(SnowLayerBlock.LAYERS) < 8;

            if (!(level.isEmptyBlock(pos) || canStackSnow)) continue;


            if (level.getBlockState(below).is(Blocks.WATER)) continue;


            if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
                ts.recordSuccess(center);
                return pos.immutable();
            }
        }


        ts.recordFail(now, center);
        return null;
    }
    /**
     * Places a snow layer at the given position if empty and valid, or adds a
     * layer to an existing snow block that is below max height.
     */
    public static void placeSnowAt(ServerLevel level, BlockPos pos) {
        final BlockState stateAt = level.getBlockState(pos);


        if (stateAt.is(Blocks.SNOW) && stateAt.hasProperty(SnowLayerBlock.LAYERS)) {
            int layers = stateAt.getValue(SnowLayerBlock.LAYERS);
            if (layers < 8) {
                level.setBlockAndUpdate(pos, stateAt.setValue(SnowLayerBlock.LAYERS, layers + 1));
            }
            return;
        }


        if (!level.isEmptyBlock(pos)) return;

        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (!snow.canSurvive(level, pos)) return;
        level.setBlockAndUpdate(pos, snow);
    }


    /**
     * Per-player throttle state to avoid repeated failed scans when the
     * player has not moved and conditions are unfavorable.
     */
    protected static final class ThrottleState {
        protected long nextAllowedTick = 0L;
        protected BlockPos lastCenter = BlockPos.ZERO;

        /**
         * Whether to skip a scan until after the throttle delay unless the
         * player has moved far enough.
         */
        boolean shouldSkip(long now, BlockPos currentCenter, int radius) {
            if (hasMovedEnough(currentCenter, radius)) {

                nextAllowedTick = 0L;
                return false;
            }
            return now < nextAllowedTick;
        }

        /**
         * Records a failed scan, scheduling the next allowed attempt.
         */
        void recordFail(long now, BlockPos center) {
            this.lastCenter = center;
            this.nextAllowedTick = now + THROTTLE_TICKS;
        }

        /**
         * Records a successful scan and clears throttling.
         */
        void recordSuccess(BlockPos center) {
            this.lastCenter = center;
            this.nextAllowedTick = 0L;
        }

        /**
         * Whether the player has moved far enough from the last scan center to
         * reset throttling.
         */
        protected boolean hasMovedEnough(BlockPos current, int radius) {
            if (lastCenter == BlockPos.ZERO) return true;
            final int dx = current.getX() - lastCenter.getX();
            final int dy = current.getY() - lastCenter.getY();
            final int dz = current.getZ() - lastCenter.getZ();
            return (dx * dx + dy * dy + dz * dz) > (radius * radius);
        }
    }





    /**
     * Quick helper to check if a snow layer could survive at pos in a generic LevelReader.
     */
    public static boolean canSnowSurvive(LevelReader level, BlockPos pos) {
        return Blocks.SNOW.defaultBlockState().canSurvive(level, pos);
    }
}

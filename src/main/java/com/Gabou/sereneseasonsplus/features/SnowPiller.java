package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

 * SnowPiller.tick(level, player, player.blockPosition(), 16);
 * <p>
 * Public API:
 * - tick(...) -> tries to find and place snow once; returns true if placed.
 * - findTarget(...) -> returns a valid position (or null) without placing.
 * - placeSnowAt(...) -> places snow with survival checks; returns success.
 */
public final class SnowPiller {

    private static final Logger LOGGER = LogUtils.getLogger();


    private static int tickThresholdSnowPiller;

    /**
     * Radius in blocks the player must move to reset throttle.
     */
    private static final int MOVEMENT_RESET_RADIUS = 10;
    /**
     * 10 seconds at 20 tps.
     */
    private static final int THROTTLE_TICKS = 200;
    /**
     * Sampling attempts per call. Tune as desired.
     */
    private static final int MAX_ATTEMPTS = 12;

    private static int tickCounter = 0;

    private static final Map<UUID, ThrottleState> THROTTLE = new ConcurrentHashMap<>();

    /**
     * Constructs a new instance.
     */
    private SnowPiller() {
    }


    /**
     * TODO: describe method.
     *
     * @param event description
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        tickThresholdSnowPiller = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        tickCounter = 0;
        THROTTLE.clear();
    }

    /**
     * TODO: describe method.
     *
     * @param event description
     */
    @SubscribeEvent
    public static void onConfigReload(TickEvent.ServerTickEvent event) {
        tickThresholdSnowPiller = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        SereneService.reloadConfig();

    }

    /**
     * TODO: describe method.
     *
     * @param event description
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && EnvironmentHelper.shouldRunMod()) {
            MinecraftServer server = event.getServer();
            Level level = server.getLevel(Level.OVERWORLD);
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

    }

     /**
      * TODO: describe method.
      *
      * @param failures description
      * @return description
      */
     * @param player the player (used to throttle repeated failures)
     /**
      * TODO: describe method.
      *
      * @param pos description
      * @return description
      */
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
     * Returns null if throttled or no position found.
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
     * Places a snow layer at pos with basic survival checks.
     * Returns true if the block was placed.
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





    private static final class ThrottleState {
        private long nextAllowedTick = 0L;
        private BlockPos lastCenter = BlockPos.ZERO;

        /**
         * TODO: describe method.
         *
         * @param now description
         * @param currentCenter description
         * @param radius description
         * @return description
         */
        boolean shouldSkip(long now, BlockPos currentCenter, int radius) {
            if (hasMovedEnough(currentCenter, radius)) {
                
                nextAllowedTick = 0L;
                return false;
            }
            return now < nextAllowedTick;
        }

        /**
         * TODO: describe method.
         *
         * @param now description
         * @param center description
         */
        void recordFail(long now, BlockPos center) {
            this.lastCenter = center;
            this.nextAllowedTick = now + THROTTLE_TICKS;
        }

        /**
         * TODO: describe method.
         *
         * @param center description
         */
        void recordSuccess(BlockPos center) {
            this.lastCenter = center;
            this.nextAllowedTick = 0L;
        }

        /**
         * TODO: describe method.
         *
         * @param current description
         * @param radius description
         * @return description
         */
        private boolean hasMovedEnough(BlockPos current, int radius) {
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

package com.Gabou.sereneseasonsplus.features;


import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.SereneService;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SnowPiller {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static int tickThresholdSnowPiller;
    private static int tickCounter = 0;

    private static final int MOVEMENT_RESET_RADIUS = 10;
    private static final int THROTTLE_TICKS = 200;
    private static final int MAX_ATTEMPTS = 12;

    private static final Map<UUID, ThrottleState> THROTTLE = new ConcurrentHashMap<>();

    private SnowPiller() {
    }

    public static void registerEvents() {
        // Fabric hook for server start
        ServerLifecycleEvents.SERVER_STARTING.register(SnowPiller::onServerStarting);

        // Fabric hook for tick
        ServerTickEvents.END_SERVER_TICK.register(SnowPiller::onServerTick);
    }

    private static void onServerStarting(MinecraftServer server) {
        tickThresholdSnowPiller = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        tickCounter = 0;
        THROTTLE.clear();
        LOGGER.info("SnowPiller initialized with tick threshold {}", tickThresholdSnowPiller);
    }

    private static void onServerTick(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) return;

        ++tickCounter;
        if (tickCounter % tickThresholdSnowPiller == 0) {
            SereneService.runAsync(() -> {
                for (ServerPlayer player : level.players()) {
                    tickSnow(level, player, player.blockPosition(), 16);
                }
            });
        }
    }

    public static void tickSnow(ServerLevel level, ServerPlayer player, BlockPos center, int radius) {
        int attempts;
        if (!level.isRaining()) return;
        if (!level.getBiome(center).value().coldEnoughToSnow(center,center.getY())) return;

        RandomSource rnd = level.random;
        attempts = rnd.nextInt(2) + (level.isThundering() ? rnd.nextInt(2) : 0);


        if (attempts <= 0) return;

        for (int i = 0; i < attempts; i++) {
            BlockPos target = findTarget(level, player, center, radius);
            if (target != null) placeSnowAt(level, target);
        }
    }

    public static @Nullable BlockPos findTarget(ServerLevel level, ServerPlayer player, BlockPos center, int radius) {
        long now = level.getGameTime();
        UUID pid = player.getUUID();

        ThrottleState ts = THROTTLE.computeIfAbsent(pid, k -> new ThrottleState());
        if (ts.shouldSkip(now, center, MOVEMENT_RESET_RADIUS)) return null;

        if (!level.getBiome(center).value().coldEnoughToSnow(center,center.getY())) {
            ts.recordFail(now, center);
            return null;
        }

        RandomSource random = level.random;
        MutableBlockPos pos = new MutableBlockPos();
        MutableBlockPos below = new MutableBlockPos();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            pos.set(x, y, z);
            below.set(x, y - 1, z);

            BlockState stateAt = level.getBlockState(pos);
            boolean canStackSnow = stateAt.is(Blocks.SNOW)
                    && stateAt.hasProperty(SnowLayerBlock.LAYERS)
                    && stateAt.getValue(SnowLayerBlock.LAYERS) < 8;

            if (!(level.isEmptyBlock(pos) || canStackSnow)) continue;
            if (level.getBlockState(below).is(Blocks.WATER)) continue;

            if (level.getBiome(pos).value().coldEnoughToSnow(pos,pos.getY())) {
                ts.recordSuccess(center);
                return pos.immutable();
            }
        }

        ts.recordFail(now, center);
        return null;
    }

    public static void placeSnowAt(ServerLevel level, BlockPos pos) {
        BlockState stateAt = level.getBlockState(pos);

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

        boolean shouldSkip(long now, BlockPos currentCenter, int radius) {
            if (hasMovedEnough(currentCenter, radius)) {
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
            int dx = current.getX() - lastCenter.getX();
            int dy = current.getY() - lastCenter.getY();
            int dz = current.getZ() - lastCenter.getZ();
            return (dx * dx + dy * dy + dz * dz) > (radius * radius);
        }
    }

    public static boolean canSnowSurvive(LevelReader level, BlockPos pos) {
        return Blocks.SNOW.defaultBlockState().canSurvive(level, pos);
    }
}

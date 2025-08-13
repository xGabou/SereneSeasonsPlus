package com.Gabou.sereneseasonsextended.features;

import com.Gabou.sereneseasonsextended.util.EnvironmentHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gradually piles up snow blocks during snowfall.
 */
@EventBusSubscriber
public class SnowPiller {
    private static final Logger LOGGER = LogManager.getLogger("SnowPiller");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> PLAYER_POSITIONS = new HashMap<>();
    private static final int UPDATE_INTERVAL = 100;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END && EnvironmentHelper.shouldRunMod()) {
            ++tickCounter;
            MinecraftServer server = event.getServer();
            Level level = server.getLevel(Level.OVERWORLD);
            if (level != null && tickCounter % UPDATE_INTERVAL == 0) {
                updatePlayerPositions(server.getPlayerList().getPlayers());
                pileSnow(level);
            }
        }
    }

    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            PLAYER_POSITIONS.put(player, player.blockPosition());
        }
    }

    private static void pileSnow(Level level) {
        for (Map.Entry<ServerPlayer, BlockPos> entry : PLAYER_POSITIONS.entrySet()) {
            ServerPlayer player = entry.getKey();
            BlockPos playerPos = entry.getValue();
            if (!level.isRainingAt(playerPos) || !level.getBiome(playerPos).value().coldEnoughToSnow(playerPos)) {
                continue;
            }

            int radius = getSimulationDistance(player) * 16;
            for (int i = 0; i < 5; ++i) {
                BlockPos target = findPlaceForSnow(level, playerPos, radius);
                if (target == null) {
                    break;
                }

                if (level.isRainingAt(target)) {
                    boolean placed = level.setBlock(target, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
                    if (placed) {
                        LOGGER.debug("Placed snow block at {}", target);
                    }
                }
            }
        }
    }

    private static BlockPos findPlaceForSnow(Level level, BlockPos center, int radius) {
        for (int attempt = 0; attempt < 20; ++attempt) {
            int x = center.getX() + RANDOM.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + RANDOM.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.isEmptyBlock(pos)) {
                return pos;
            }
        }
        return null;
    }

    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }
}


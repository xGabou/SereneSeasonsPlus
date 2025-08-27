//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlus;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.Gabou.sereneseasonsplus.util.SereneService;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

@EventBusSubscriber
public class SnowBlockReplacer {
    private static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> playerPositions = new HashMap();

    private static int tickThresholdSnowReplacer;
    private static int tickThresholdSnowReplacerForHotSeasons = 30;
    private static int tickCounter = 0;


    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        tickCounter = 0;
        playerPositions.clear();
        LOGGER.info("SnowBlockReplacer initialized with tick threshold: {}", tickThresholdSnowReplacer);
    }


    @SubscribeEvent
    public static void onConfigReload(TickEvent.ServerTickEvent event) {
        tickThresholdSnowReplacer = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        SereneService.reloadConfig();

    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END && EnvironmentHelper.shouldRunMod()) {
            ++tickCounter;
            MinecraftServer server = event.getServer();
            Level level = server.getLevel(Level.OVERWORLD);
            if (level != null && !level.isClientSide()) {
                if (tickCounter % tickThresholdSnowReplacer == 0 || (tickCounter % tickThresholdSnowReplacerForHotSeasons == 0) && EnvironmentHelper.isHotSeason()) {
                    updatePlayerPositions(server.getPlayerList().getPlayers());
                    SereneService.runAsync(() -> replaceSnowBlocks(level));
                }

            }
        }
    }

    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }

    }

    private static void replaceSnowBlocks(Level level) {
        Iterator var1 = playerPositions.entrySet().iterator();

        while (true) {
            BlockPos playerPos;
            int radius;
            int blocksToReplace;
            while (true) {
                if (!var1.hasNext()) {
                    return;
                }

                Map.Entry<ServerPlayer, BlockPos> entry = (Map.Entry) var1.next();
                ServerPlayer player = (ServerPlayer) entry.getKey();
                playerPos = (BlockPos) entry.getValue();
                int simulationDistance = getSimulationDistance(player);
                radius = Mth.clamp(simulationDistance * 16,16,64);
                if (!SereneSeasonsPlus.isProjectAtmosphereLoaded) {
                    Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                    float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);
                    if (!(temperature < 0.15F)) {
                        blocksToReplace = calculateBlocksToReplace(temperature);
                        break;
                    }
                } else {
                    float temperature = ForecastOrchestrator.getCurrentTemperature(new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos), level.getDayTime());
                    if (!((double) temperature < (double) 0.5F)) {
                        blocksToReplace = calculateBlocksToReplace1(temperature);
                        break;
                    }
                }
            }

            for (int i = 0; i < blocksToReplace; ++i) {
                BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                if (targetPos == null) {
                    break;
                }

                SnowUtils.breakOrDecrementLayer(level,targetPos);
            }
        }
    }

    private static int calculateBlocksToReplace1(float temperature) {
        return (int) Math.ceil((double) (temperature / 5.0F));
    }


    private static int calculateBlocksToReplace(float temperature) {
        if (temperature < 0.2F) {
            return 1;
        } else {
            return temperature < 0.5F ? 3 : 5;
        }
    }

    private static int getSimulationDistance(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.getPlayerList().getViewDistance() : 10;
    }

    private static BlockPos findSnowBlockInRadius(Level level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                for (int y = -5; y <= 5; ++y) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.SNOW_BLOCK)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }


}

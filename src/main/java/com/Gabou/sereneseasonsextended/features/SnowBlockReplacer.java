//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.Gabou.sereneseasonsextended.features;

import com.Gabou.sereneseasonsextended.SereneSeasonsExtended;
import com.Gabou.sereneseasonsextended.util.EnvironmentHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import net.Gabou.projectatmosphere.modules.temperature.util.TemperatureProfileManager;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.api.season.Season.SubSeason;

@EventBusSubscriber
public class SnowBlockReplacer {
    private static final Logger LOGGER = LogManager.getLogger("SnowBlockReplacer");
    private static final Random RANDOM = new Random();
    private static final Map<ServerPlayer, BlockPos> playerPositions = new HashMap();
    private static final Map<String, Float> biomeTemperatures = new HashMap();
    private static final int UPDATE_INTERVAL = 100;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == Phase.END && EnvironmentHelper.shouldRunMod()) {
            ++tickCounter;
            MinecraftServer server = event.getServer();
            Level level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                if (tickCounter % 100 == 0) {
                    updatePlayerPositions(server.getPlayerList().getPlayers());
                    replaceSnowBlocks(level);
                }

            }
        }
    }

    private static void updatePlayerPositions(Iterable<ServerPlayer> players) {
        for(ServerPlayer player : players) {
            playerPositions.put(player, player.blockPosition());
        }

    }

    private static void replaceSnowBlocks(Level level) {
        Iterator var1 = playerPositions.entrySet().iterator();

        while(true) {
            BlockPos playerPos;
            int radius;
            int blocksToReplace;
            while(true) {
                if (!var1.hasNext()) {
                    return;
                }

                Map.Entry<ServerPlayer, BlockPos> entry = (Map.Entry)var1.next();
                ServerPlayer player = (ServerPlayer)entry.getKey();
                playerPos = (BlockPos)entry.getValue();
                int simulationDistance = getSimulationDistance(player);
                radius = simulationDistance * 16;
                if (!SereneSeasonsExtended.isProjectAtmosphereLoaded) {
                    Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
                    float temperature = getCachedBiomeTemperature(level, playerPos, currentSubSeason);
                    if (!(temperature < 0.15F)) {
                        blocksToReplace = calculateBlocksToReplace(temperature);
                        break;
                    }
                } else {
                    float temperature = TemperatureProfileManager.getCurrentTemperature(new BiomeInstanceKey(((ResourceKey)level.getBiome(playerPos).unwrapKey().get()).location(), playerPos), level.getDayTime());
                    if (!((double)temperature < (double)0.5F)) {
                        blocksToReplace = calculateBlocksToReplace1(temperature);
                        break;
                    }
                }
            }

            for(int i = 0; i < blocksToReplace; ++i) {
                BlockPos targetPos = findSnowBlockInRadius(level, playerPos, radius);
                if (targetPos == null) {
                    break;
                }

                boolean blockReplaced = level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                if (blockReplaced) {
                    LOGGER.debug("Replaced snow block at {}", targetPos);
                }
            }
        }
    }

    private static int calculateBlocksToReplace1(float temperature) {
        return (int)Math.ceil((double)(temperature / 5.0F));
    }

    private static float getCachedBiomeTemperature(Level level, BlockPos pos, Season.SubSeason currentSubSeason) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        String biomeName = (String)biomeHolder.unwrapKey().map(Object::toString).orElse("unknown");
        if (!biomeTemperatures.containsKey(biomeName)) {
            float temperature = getBiomeTemperature(level, biomeHolder, pos);
            if (isWinterSubSeason(currentSubSeason) && temperature > 0.14F) {
                temperature = 0.14F;
            }

            biomeTemperatures.put(biomeName, temperature);
            LOGGER.info("Biome: {}, Temperature: {}", biomeName, temperature);
            return temperature;
        } else {
            float cachedTemperature = (Float)biomeTemperatures.get(biomeName);
            if (!isWinterSubSeason(currentSubSeason)) {
                float newTemperature = getBiomeTemperature(level, biomeHolder, pos);
                if (newTemperature != cachedTemperature || cachedTemperature <= 0.14F) {
                    biomeTemperatures.put(biomeName, newTemperature);
                    LOGGER.info("Biome: {}, Updated Temperature: {}", biomeName, newTemperature);
                    return newTemperature;
                }
            }

            if (isWinterSubSeason(currentSubSeason) && cachedTemperature > 0.14F) {
                cachedTemperature = 0.14F;
                biomeTemperatures.put(biomeName, cachedTemperature);
                LOGGER.info("Biome: {}, Reset Temperature to Winter: {}", biomeName, cachedTemperature);
            }

            return cachedTemperature;
        }
    }

    public static float getBiomeTemperature(LevelReader level, Holder<Biome> biomeHolder, BlockPos pos) {
        Biome biome = (Biome)biomeHolder.value();
        return biome.getBaseTemperature();
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
        for(int x = -radius; x <= radius; ++x) {
            for(int z = -radius; z <= radius; ++z) {
                for(int y = -5; y <= 5; ++y) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.SNOW_BLOCK)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isWinterSubSeason(Season.SubSeason subSeason) {
        return subSeason == SubSeason.EARLY_WINTER || subSeason == SubSeason.MID_WINTER || subSeason == SubSeason.LATE_WINTER;
    }
}

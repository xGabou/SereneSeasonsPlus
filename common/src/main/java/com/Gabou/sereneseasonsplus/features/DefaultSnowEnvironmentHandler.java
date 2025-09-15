package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

import static com.Gabou.sereneseasonsplus.features.CommonSnowBlockReplacer.*;

public class DefaultSnowEnvironmentHandler implements SnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        Season.SubSeason currentSubSeason = SeasonHelper.getSeasonState(level).getSubSeason();
        float temperature = SnowUtils.getCachedBiomeTemperature(level, playerPos, currentSubSeason);

        if (temperature >= 0.15F) {
            return CommonSnowBlockReplacer.calculateBlocksToReplace(temperature);
        }
        return 0;
    }

    @Override
    public void processChunks(ServerLevel level, BlockPos worldPos, Season.SubSeason sub, ChunkPos chunkPos) {
        doMeltingLogic(level, sub, chunkPos, SnowUtils.getCachedBiomeTemperature(level, worldPos, sub));
    }

    public static void doMeltingLogic(ServerLevel level, Season.SubSeason sub, ChunkPos chunkPos, float temperature) {
        switch (sub) {
            case EARLY_SPRING, LATE_AUTUMN -> {
                if (temperature >= 0.15F) {
                    // gradual only
                    meltingChunks.add(chunkPos);
                }
            }
            case MID_SPRING, MID_AUTUMN -> {
                if (temperature >= 0.15F && temperature < 0.5F) {
                    // gradual + accelerated melt
                    level.getServer().execute(() -> accelerateMelt(level, chunkPos));
                    meltingChunks.add(chunkPos);
                } else if (temperature >= 0.5F) {
                    // accelerated only
                    level.getServer().execute(() -> accelerateMelt(level, chunkPos));
                }
            }
            case LATE_SPRING, EARLY_AUTUMN,
                 EARLY_SUMMER, MID_SUMMER, LATE_SUMMER -> {
                if (temperature >= 0.5F) {
                    // hot enough to wipe snow
                    chunksToClear.add(chunkPos);
                } else if (temperature >= 0.15F) {
                    // still melting zone
                    meltingChunks.add(chunkPos);
                }
            }
            default -> {
                // Winter or too cold → keep snow
            }
        }
    }
}

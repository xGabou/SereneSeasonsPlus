package com.Gabou.sereneseasonsplus.features;
import com.Gabou.sereneseasonsplus.SereneSeasonsPlusForge;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.util.SnowUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import net.Gabou.projectatmosphere.manager.ForecastOrchestrator;
import net.Gabou.projectatmosphere.util.BiomeInstanceKey;
import sereneseasons.season.SeasonHooks;

public class ForgeSnowEnvironmentHandler extends DefaultSnowEnvironmentHandler {
    @Override
    public int getBlocksToReplace(ServerLevel level, BlockPos playerPos) {
        if (!SereneSeasonsPlusForge.isProjectAtmosphereLoaded) {
            float temperature = SeasonHooks.getBiomeTemperature(level,level.getBiome(playerPos),playerPos);
            if (SeasonHooks.coldEnoughToSnowSeasonal(level, playerPos)) {
                return CommonSnowBlockFeature.calculateBlocksToReplace(temperature);
            }

        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(playerPos).unwrapKey().get().location(), playerPos),
                    level.getDayTime()
            );

            if (temperature >= 0.5F) {
                return CommonSnowBlockFeature.calculateBlocksToReplace1(temperature);
            }
        }
        return -level.random.nextInt(2,6);
    }

    @Override
    public boolean isColdEnoughForSnow(ServerLevel level, BlockPos pos) {
        if (!SereneSeasonsPlusForge.isProjectAtmosphereLoaded) {
            return SeasonHooks.coldEnoughToSnowSeasonal(level, pos);
        } else {
            float temperature = ForecastOrchestrator.getCurrentTemperature(
                    new BiomeInstanceKey(level.getBiome(pos).unwrapKey().get().location(), pos),
                    level.getDayTime()
            );
            return temperature < 0.5F;
        }
    }


}

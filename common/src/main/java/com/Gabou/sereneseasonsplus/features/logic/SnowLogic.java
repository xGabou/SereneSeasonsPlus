package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;

/**
 * Thin scheduling bridge between chunk evaluation and the chunk queue.
 */
public final class SnowLogic {

    private SnowLogic() {}

    public static void evaluate(ServerLevel level,
                                Season.SubSeason currentSeason,
                                ISeasonState seasonState,
                                ISnowTrackedChunk tracked,
                                ChunkPos chunkPos,
                                boolean isLoadEvent,
                                int maxHeight) {
        var decision = CommonSnowBlockFeature.SNOW_ACCUMULATION_POLICY.evaluateChunk(
                level,
                currentSeason,
                tracked,
                chunkPos,
                isLoadEvent,
                maxHeight,
                CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(
                        level,
                        chunkPos.getMiddleBlockPosition(Math.max(level.getMinBuildHeight(), maxHeight))
                )
        );

        if (decision.action() == SnowAccumulationPolicy.Action.APPLY) {
            CommonSnowBlockFeature.enqueueChunkForSnowApply(chunkPos, currentSeason);
        } else if (decision.action() == SnowAccumulationPolicy.Action.MELT) {
            CommonSnowBlockFeature.enqueueChunkForSnowMelt(chunkPos, decision.fullClear());
        }
    }
}

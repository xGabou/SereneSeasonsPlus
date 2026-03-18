package com.Gabou.sereneseasonsplus.features.logic;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.injection.Inject;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonHooks;

/**
 * Shared server-side snow/ice tick logic invoked from multiple mixins.
 */
public final class SnowChunkWeatherLogic {
    private SnowChunkWeatherLogic() {
    }

    public static void run(ServerLevel level, LevelChunk chunk) {
        if (level == null || chunk == null || level.isClientSide()) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (CommonSnowBlockFeature.isSnowFeatureEnabled()) {
            int t = CommonSnowBlockFeature.getTickCounter();
            ChunkPos cpos = chunk.getPos();
            boolean doEval = ((cpos.x ^ cpos.z ^ t) & 15) == 0; // ~once per 16 ticks per ticking chunk
            if (doEval && chunk instanceof ISnowTrackedChunk tracked) {
                Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
                var seasonState = SeasonHelper.getSeasonState(level);

                if (seasonState != null && currentSeason != null) {
                    // Lazily cache surface height if not set yet
                    if (tracked.sereneseasonsplus$getSurfaceHeight() == -1) {
                        int surfaceHeightCache = level.getHeight(
                                Heightmap.Types.WORLD_SURFACE,
                                cpos.getMiddleBlockX(),
                                cpos.getMiddleBlockZ()
                        );
                        tracked.sereneseasonsplus$setSurfaceHeight(surfaceHeightCache);
                    }
                    int surfaceHeight = tracked.sereneseasonsplus$getSurfaceHeight();
                    SnowLogic.evaluate(level, currentSeason, seasonState, tracked, cpos, true, surfaceHeight);
                }
            }
        }

        ProfilerFiller profiler = level.getProfiler();
        profiler.popPush("iceandsnow");
        if (level.random.nextInt(16) == 0) {
            boolean snowRealMagicLoaded = EnvironmentHelper.isSnowRealMagicLoaded();
            int j = chunk.getPos().getMinBlockX();
            int k = chunk.getPos().getMinBlockZ();
            BlockPos blockPos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    level.getBlockRandomPos(j, 0, k, 15)
            );
            BlockPos blockPos2 = blockPos.below();
            Biome biome = level.getBiome(blockPos).value();
            if (CommonSnowBlockFeature.HANDLER.isColdEnoughForSnow(level, blockPos)) {//done
                CommonSnowBlockFeature.tryFreezeWaterAt(level, blockPos2);
            }

            // Snow accumulation (uses your helper instead of vanilla "bl")
            if (!snowRealMagicLoaded && EnvironmentHelper.isRainning(level, blockPos) && level.canSeeSkyFromBelowWater(blockPos)) {
                int maxSnow = CommonSnowBlockFeature.getSnowHeightCap();
                if (maxSnow > 0 && SeasonHooks.shouldSnowHook(biome, level, blockPos)) {
                    BlockState state = level.getBlockState(blockPos);
                    // Skip if this column was marked destroyed for the current storm
                    boolean skipDueToDestroyed = false;
                    com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData sd = com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData.get();
                    int activeId = (sd != null) ? sd.currentStormId : 0;
                    if (activeId > 0 && chunk instanceof ISnowTrackedChunk tracked) {
                        if (tracked.sereneseasonsplus$getDestroyedStormId() != activeId) {
                            tracked.sereneseasonsplus$getDestroyedColumns().clear();
                            tracked.sereneseasonsplus$setDestroyedStormId(activeId);
                        }
                        long xz = (((long) blockPos.getX()) << 32) ^ (blockPos.getZ() & 0xffffffffL);
                        if (tracked.sereneseasonsplus$getDestroyedColumns().contains(xz)) {
                            skipDueToDestroyed = true;
                        }
                    }
                    if (skipDueToDestroyed) {
                        return; // abort vanilla-like snow add in this column
                    }

                    if (state.is(Blocks.SNOW)) {
                        int layers = state.getValue(SnowLayerBlock.LAYERS);
                        if (layers < Math.min(maxSnow, 8)) {
                            BlockState next = state.setValue(SnowLayerBlock.LAYERS, layers + 1);
                            Block.pushEntitiesUp(state, next, level, blockPos);
                            if (level.setBlockAndUpdate(blockPos, next)) {
                                CommonSnowBlockFeature.accumulateColumnUpdate(level, blockPos, next);
                            }
                        }
                    } else {
                        BlockState snow = Blocks.SNOW.defaultBlockState();
                        if (level.setBlockAndUpdate(blockPos, snow)) {
                            CommonSnowBlockFeature.accumulateColumnUpdate(level, blockPos, snow);//done
                        }
                    }
                }
                //same for ice accumulation
                // Precipitation hook
                Biome.Precipitation precipitation = biome.getPrecipitationAt(blockPos2);
                if (precipitation != Biome.Precipitation.NONE) {
                    BlockState base = level.getBlockState(blockPos2);
                    base.getBlock().handlePrecipitation(base, level, blockPos2, precipitation);
                }
            }
            if (snowRealMagicLoaded && EnvironmentHelper.isRainning(level, blockPos)) {
                Biome.Precipitation precipitation = biome.getPrecipitationAt(blockPos2);
                if (precipitation != Biome.Precipitation.NONE) {
                    BlockState base = level.getBlockState(blockPos2);
                    base.getBlock().handlePrecipitation(base, level, blockPos2, precipitation);
                }
            }
        }
    }
}




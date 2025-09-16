package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(
            method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            ),
            remap = false
    )
    private void snow$addToQueue(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if(CommonSnowBlockFeature.getTickCounter()<500)
            return;
        if(!(chunk.getLevel() instanceof ServerLevel level)){
            return;
        }
        if (level.dimension() != Level.OVERWORLD) return;

        ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
        Season.SubSeason currentSeason = SeasonHelper.getSeasonState(level).getSubSeason();

        // First load or season changed
        if (tracked.sereneseasonsplus$getLastSeason() != currentSeason) {
            tracked.sereneseasonsplus$setLastSeason(currentSeason);
            tracked.sereneseasonsplus$setNeedsSnowUpdate(true);
        }

        // Rain change (or PA override)
        boolean isRaining = EnvironmentHelper.isRainning(level); // or level.isSnowStormAt(chunkPos) if PA present
        if (isRaining != tracked.sereneseasonsplus$wasRaining()) {
            tracked.sereneseasonsplus$setWasRaining(isRaining);
            tracked.sereneseasonsplus$setNeedsSnowUpdate(true);
        }

        if (tracked.sereneseasonsplus$needsSnowUpdate()) {
            ChunkQueue.tryAdd(chunk.getPos(), true);
            tracked.sereneseasonsplus$setNeedsSnowUpdate(false);
        }
    }




}
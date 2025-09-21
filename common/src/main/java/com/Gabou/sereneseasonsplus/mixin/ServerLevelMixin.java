package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Unique
    private static final int MIN_TICKS_INTERVALLES = 10;

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void snow$addToQueue(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if(CommonSnowBlockFeature.getTickCounter()% MIN_TICKS_INTERVALLES !=0) return;

        if(!(chunk.getLevel() instanceof ServerLevel level)){
            return;
        }
        if (level.dimension() != Level.OVERWORLD) return;

        ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();

        if (tracked.sereneseasonsplus$getLastSeason() != currentSeason) {
            tracked.sereneseasonsplus$setLastSeason(currentSeason);
        }

        boolean isRaining = EnvironmentHelper.isRainning(level,chunk.getPos().getMiddleBlockPosition(65));
        if (isRaining != tracked.sereneseasonsplus$wasRaining()) {
            tracked.sereneseasonsplus$setWasRaining(isRaining);
            if (!isRaining) {
                tracked.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(false);
            }
        }
    }




}

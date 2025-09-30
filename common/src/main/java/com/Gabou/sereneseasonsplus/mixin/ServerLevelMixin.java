package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.logic.SnowLogic;
import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import com.Gabou.sereneseasonsplus.util.EnvironmentHelper;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Shadow
    @Final
    private static Logger LOGGER;
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
        if (CommonSnowBlockFeature.getTickCounter() % MIN_TICKS_INTERVALLES != 0) return;
        if (!(chunk.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        ISnowTrackedChunk tracked = (ISnowTrackedChunk) chunk;
        Season.SubSeason currentSeason = EnvironmentHelper.getCurrentSeason();
        var seasonState = SeasonHelper.getSeasonState(level);
        if (seasonState == null || currentSeason == null) return;

        SnowLogic.evaluate(level, currentSeason, seasonState, tracked, chunk.getPos(), false,chunk.getHeight());
    }


}

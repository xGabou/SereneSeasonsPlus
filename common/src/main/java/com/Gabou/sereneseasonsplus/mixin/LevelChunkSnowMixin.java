package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import sereneseasons.api.season.Season;

@Mixin(LevelChunk.class)
public class LevelChunkSnowMixin implements ISnowTrackedChunk {
    @Unique
    private boolean sereneseasonsplus$snowNeedsUpdate = true;
    @Unique
    private Season.SubSeason sereneseasonsplus$lastSeason = null;
    @Unique
    private boolean sereneseasonsplus$wasRaining = false;
    @Unique
    private boolean sereneseasonsplus$hasReceivedSnowLayerThisStorm = false;

    @Override
    public boolean sereneseasonsplus$needsSnowUpdate() {
        return sereneseasonsplus$snowNeedsUpdate;
    }

    @Override
    public void sereneseasonsplus$setNeedsSnowUpdate(boolean needsUpdate) {
        this.sereneseasonsplus$snowNeedsUpdate = needsUpdate;
    }

    @Override
    public Season.SubSeason sereneseasonsplus$getLastSeason() {
        return sereneseasonsplus$lastSeason;
    }

    @Override
    public void sereneseasonsplus$setLastSeason(Season.SubSeason season) {
        this.sereneseasonsplus$lastSeason = season;
    }

    @Override
    public boolean sereneseasonsplus$wasRaining() {
        return sereneseasonsplus$wasRaining;
    }

    @Override
    public void sereneseasonsplus$setWasRaining(boolean raining) {
        this.sereneseasonsplus$wasRaining = raining;
    }

    @Override
    public boolean sereneseasonsplus$hasReceivedSnowLayerThisStorm() {
        return sereneseasonsplus$hasReceivedSnowLayerThisStorm;
    }

    @Override
    public void sereneseasonsplus$setHasReceivedSnowLayerThisStorm(boolean value) {
        this.sereneseasonsplus$hasReceivedSnowLayerThisStorm = value;
    }
}


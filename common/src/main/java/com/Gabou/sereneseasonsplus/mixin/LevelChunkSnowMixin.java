package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.api.season.Season;

import javax.annotation.Nullable;

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
    @Unique
    private boolean sereneseasonsplus$shouldApplyInitialSnow = false;
    @Unique
    private boolean sereneseasonsplus$hasAppliedInitialSnow = false;

    // NEW: track how many times this chunk has snowed
    @Unique
    private int sereneseasonsplus$snowCount = 0;

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

    @Override
    public boolean sereneseasonsplus$shouldApplyInitialSnow() {
        return sereneseasonsplus$shouldApplyInitialSnow;
    }

    @Override
    public void sereneseasonsplus$setShouldApplyInitialSnow(boolean value) {
        this.sereneseasonsplus$shouldApplyInitialSnow = value;
    }

    @Override
    public boolean sereneseasonsplus$hasAppliedInitialSnow() {
        return sereneseasonsplus$hasAppliedInitialSnow;
    }

    @Override
    public void sereneseasonsplus$setHasAppliedInitialSnow(boolean value) {
        this.sereneseasonsplus$hasAppliedInitialSnow = value;
    }

    // --- New accessors for snow count ---
    @Override
    public int sereneseasonsplus$getSnowCount() {
        return sereneseasonsplus$snowCount;
    }

    @Override
    public void sereneseasonsplus$incrementSnowCount() {
        sereneseasonsplus$snowCount++;
    }

    @Override
    public void sereneseasonsplus$setSnowCount(int value) {
        this.sereneseasonsplus$snowCount = value;
    }

    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("TAIL")
    )
    private void ssp$copy(ServerLevel level,
                          ProtoChunk proto,
                          @Nullable LevelChunk.PostLoadProcessor post,
                          CallbackInfo ci) {
        if ((Object)this instanceof ISnowTrackedChunk target && proto instanceof ISnowTrackedChunk src) {
            target.sereneseasonsplus$setSnowCount(src.sereneseasonsplus$getSnowCount());
            target.sereneseasonsplus$setHasAppliedInitialSnow(src.sereneseasonsplus$hasAppliedInitialSnow());
            target.sereneseasonsplus$setShouldApplyInitialSnow(src.sereneseasonsplus$shouldApplyInitialSnow());
            target.sereneseasonsplus$setWasRaining(src.sereneseasonsplus$wasRaining());
            target.sereneseasonsplus$setHasReceivedSnowLayerThisStorm(src.sereneseasonsplus$hasReceivedSnowLayerThisStorm());
            if (src.sereneseasonsplus$getLastSeason() != null) {
                target.sereneseasonsplus$setLastSeason(src.sereneseasonsplus$getLastSeason());
            }
        }
    }

}



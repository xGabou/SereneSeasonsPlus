package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(value = LevelChunk.class )
public class LevelChunkSnowMixin implements ISnowTrackedChunk {
    @Unique
    private int sereneseasonsplus$lastWinterId = -1;

    @Unique
    private final Map<BlockPos, Integer> sereneseasonsplus$snowColumns = new HashMap<>();
    @Unique
    private final Set<BlockPos> sereneseasonsplus$iceColumns = new HashSet<>();

    @Unique
    private int sereneseasonsplus$destroyedStormId = 0;
    @Unique
    private final java.util.Set<Long> sereneseasonsplus$destroyedColumns = new java.util.HashSet<>();

    /**
     * Cached surface height (first available, not average)
     */
    @Unique
    private int sereneseasonsplus$surfaceHeight = -1;

    /**
     * Estimated number of snow-placeable columns in this chunk (0..256). -1 until computed.
     */
    @Unique
    private int sereneseasonsplus$availableColumns = -1;

    // Active storm progress (0..1), storm id bound to this progress, and last tick progressed
    @Unique
    private float sereneseasonsplus$stormProgress = 0f;
    @Unique
    private int sereneseasonsplus$stormIdApplied = 0;
    @Unique
    private int sereneseasonsplus$lastProgressTick = 0;

    @Unique
    private int sereneseasonsplus$snowSyncGeneration = -1;

    @Unique
    private int sereneseasonsplus$appliedStormCount = 0;

    @Override
    public int sereneseasonsplus$getLastWinterId() {
        return sereneseasonsplus$lastWinterId;
    }

    @Override
    public void sereneseasonsplus$setLastWinterId(int id) {
        this.sereneseasonsplus$lastWinterId = id;
    }

    @Override
    public Map<BlockPos, Integer> sereneseasonsplus$getSnowColumns() {
        return sereneseasonsplus$snowColumns;
    }

    @Override
    public Set<BlockPos> sereneseasonsplus$getIceColumns() {
        return sereneseasonsplus$iceColumns;
    }

    @Override
    public int sereneseasonsplus$getDestroyedStormId() {
        return sereneseasonsplus$destroyedStormId;
    }

    @Override
    public void sereneseasonsplus$setDestroyedStormId(int id) {
        sereneseasonsplus$destroyedStormId = id;
    }

    @Override
    public java.util.Set<Long> sereneseasonsplus$getDestroyedColumns() {
        return sereneseasonsplus$destroyedColumns;
    }

    /**
     * Getter for cached surface height
     */
    @Override
    public int sereneseasonsplus$getSurfaceHeight() {
        return sereneseasonsplus$surfaceHeight;
    }

    /**
     * @param height
     */
    @Override
    public void sereneseasonsplus$setSurfaceHeight(int height) {
        sereneseasonsplus$surfaceHeight = height;
    }

    @Override
    public int sereneseasonsplus$getAvailableSnowColumns() {
        return sereneseasonsplus$availableColumns;
    }

    @Override
    public void sereneseasonsplus$setAvailableSnowColumns(int count) {
        sereneseasonsplus$availableColumns = Math.max(-1, Math.min(256, count));
    }

    @Override
    public float sereneseasonsplus$getStormProgress() {
        return sereneseasonsplus$stormProgress;
    }

    @Override
    public void sereneseasonsplus$setStormProgress(float progress) {
        sereneseasonsplus$stormProgress = progress;
    }

    @Override
    public int sereneseasonsplus$getStormIdApplied() {
        return sereneseasonsplus$stormIdApplied;
    }

    @Override
    public void sereneseasonsplus$setStormIdApplied(int id) {
        sereneseasonsplus$stormIdApplied = id;
    }

    @Override
    public int sereneseasonsplus$getLastProgressTick() {
        return sereneseasonsplus$lastProgressTick;
    }

    @Override
    public void sereneseasonsplus$setLastProgressTick(int tick) {
        sereneseasonsplus$lastProgressTick = tick;
    }

    @Override
    public int sereneseasonsplus$getSnowSyncGeneration() {
        return sereneseasonsplus$snowSyncGeneration;
    }

    @Override
    public void sereneseasonsplus$setSnowSyncGeneration(int generation) {
        sereneseasonsplus$snowSyncGeneration = generation;
    }

    @Override
    public int sereneseasonsplus$getAppliedStormCount() {
        return sereneseasonsplus$appliedStormCount;
    }

    @Override
    public void sereneseasonsplus$setAppliedStormCount(int count) {
        sereneseasonsplus$appliedStormCount = Math.max(0, count);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
    at = @At("TAIL"))
    private void ssp$copy(ServerLevel level,
                          ProtoChunk proto,
                          @Nullable LevelChunk.PostLoadProcessor post,
                          CallbackInfo ci) {
        if (proto instanceof ISnowTrackedChunk src && (Object)this instanceof ISnowTrackedChunk target) {
            target.sereneseasonsplus$setLastWinterId(src.sereneseasonsplus$getLastWinterId());
            target.sereneseasonsplus$getSnowColumns().clear();
            target.sereneseasonsplus$getSnowColumns().putAll(src.sereneseasonsplus$getSnowColumns());
            target.sereneseasonsplus$getIceColumns().clear();
            target.sereneseasonsplus$getIceColumns().addAll(src.sereneseasonsplus$getIceColumns());
            target.sereneseasonsplus$setStormProgress(src.sereneseasonsplus$getStormProgress());
            target.sereneseasonsplus$setStormIdApplied(src.sereneseasonsplus$getStormIdApplied());
            target.sereneseasonsplus$setLastProgressTick(src.sereneseasonsplus$getLastProgressTick());
            target.sereneseasonsplus$setSurfaceHeight(src.sereneseasonsplus$getSurfaceHeight());
            target.sereneseasonsplus$setAvailableSnowColumns(src.sereneseasonsplus$getAvailableSnowColumns());
            target.sereneseasonsplus$setDestroyedStormId(src.sereneseasonsplus$getDestroyedStormId());
            target.sereneseasonsplus$setSnowSyncGeneration(src.sereneseasonsplus$getSnowSyncGeneration());
            target.sereneseasonsplus$setAppliedStormCount(src.sereneseasonsplus$getAppliedStormCount());
            target.sereneseasonsplus$getDestroyedColumns().clear();
            target.sereneseasonsplus$getDestroyedColumns().addAll(src.sereneseasonsplus$getDestroyedColumns());
        }
    }

}

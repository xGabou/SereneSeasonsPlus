package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(LevelChunk.class)
public class LevelChunkSnowMixin implements ISnowTrackedChunk {
    @Unique
    private int sereneseasonsplus$lastWinterId = -1;

    @Unique
    private final Map<BlockPos, Integer> sereneseasonsplus$snowColumns = new HashMap<>();

    /**
     * Cached surface height (first available, not average)
     */
    @Unique
    private int sereneseasonsplus$surfaceHeight = -1;

    // Active storm progress (0..1), storm id bound to this progress, and last tick progressed
    @Unique
    private float sereneseasonsplus$stormProgress = 0f;
    @Unique
    private int sereneseasonsplus$stormIdApplied = 0;
    @Unique
    private int sereneseasonsplus$lastProgressTick = 0;

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

    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("TAIL")
    )
    private void ssp$copy(ServerLevel level,
                          ProtoChunk proto,
                          @Nullable LevelChunk.PostLoadProcessor post,
                          CallbackInfo ci) {
        if ((Object) this instanceof ISnowTrackedChunk target && proto instanceof ISnowTrackedChunk src) {
            target.sereneseasonsplus$setLastWinterId(src.sereneseasonsplus$getLastWinterId());
            this.sereneseasonsplus$snowColumns.clear();
            this.sereneseasonsplus$snowColumns.putAll(src.sereneseasonsplus$getSnowColumns());
            // Copy progress fields across proto->level chunk transition
            target.sereneseasonsplus$setStormProgress(src.sereneseasonsplus$getStormProgress());
            target.sereneseasonsplus$setStormIdApplied(src.sereneseasonsplus$getStormIdApplied());
            target.sereneseasonsplus$setLastProgressTick(src.sereneseasonsplus$getLastProgressTick());
        }

    }
}

package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
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
import java.util.Map;

@Mixin(LevelChunk.class)
public class LevelChunkSnowMixin implements ISnowTrackedChunk {

    @Unique
    private boolean sereneseasonsplus$shouldApplyInitialSnow = false;
    @Unique
    private boolean sereneseasonsplus$hasAppliedInitialSnow = false;

    @Unique
    private final Map<BlockPos, Integer> sereneseasonsplus$snowColumns = new HashMap<>();

    @Unique
    private int sereneseasonsplus$lastWinterId = -1; // -1 means no winter applied yet

    // --- Accessors ---

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

    @Override
    public Map<BlockPos, Integer> sereneseasonsplus$getSnowColumns() {
        return sereneseasonsplus$snowColumns;
    }

    @Override
    public int sereneseasonsplus$getLastWinterId() {
        return sereneseasonsplus$lastWinterId;
    }

    @Override
    public void sereneseasonsplus$setLastWinterId(int id) {
        this.sereneseasonsplus$lastWinterId = id;
    }

    // --- Copy from ProtoChunk into LevelChunk when promoted ---
    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("TAIL")
    )
    private void ssp$copy(ServerLevel level,
                          ProtoChunk proto,
                          @Nullable LevelChunk.PostLoadProcessor post,
                          CallbackInfo ci) {
        if ((Object)this instanceof ISnowTrackedChunk target && proto instanceof ISnowTrackedChunk src) {
            target.sereneseasonsplus$setHasAppliedInitialSnow(src.sereneseasonsplus$hasAppliedInitialSnow());
            target.sereneseasonsplus$setShouldApplyInitialSnow(src.sereneseasonsplus$shouldApplyInitialSnow());
            target.sereneseasonsplus$setLastWinterId(src.sereneseasonsplus$getLastWinterId());

            this.sereneseasonsplus$snowColumns.clear();
            this.sereneseasonsplus$snowColumns.putAll(src.sereneseasonsplus$getSnowColumns());
        }
    }
}

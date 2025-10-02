package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public class LevelChunkSnowMixin implements ISnowTrackedChunk {
    @Unique
    private int sereneseasonsplus$lastWinterId = -1;

    @Unique
    private final java.util.Map<net.minecraft.core.BlockPos, Integer> sereneseasonsplus$snowColumns = new java.util.HashMap<>();

    @Override
    public int sereneseasonsplus$getLastWinterId() {
        return sereneseasonsplus$lastWinterId;
    }

    @Override
    public void sereneseasonsplus$setLastWinterId(int id) {
        this.sereneseasonsplus$lastWinterId = id;
    }

    @Override
    public java.util.Map<net.minecraft.core.BlockPos, Integer> sereneseasonsplus$getSnowColumns() {
        return sereneseasonsplus$snowColumns;
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
        }
    }
}



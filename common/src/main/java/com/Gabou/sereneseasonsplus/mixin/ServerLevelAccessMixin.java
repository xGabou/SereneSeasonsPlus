package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.access.IServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelAccessMixin implements IServerLevel {

    @Unique
    private LevelChunk sereneseasonsplus$currentPrecipChunk;

    @Override
    public LevelChunk sereneseasonsplus$getChunkLevel() {
        return sereneseasonsplus$currentPrecipChunk;
    }

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tickPrecipitation(Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void sereneseasonsplus$capturePrecipChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        this.sereneseasonsplus$currentPrecipChunk = chunk;
    }

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tickPrecipitation(Lnet/minecraft/core/BlockPos;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void sereneseasonsplus$clearPrecipChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        this.sereneseasonsplus$currentPrecipChunk = null;
    }
}

package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.storage.ChunkQueue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(
            method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            ),remap = false
    )
    private void snow$addToQueue(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (chunk.getLevel().dimensionType().natural()) ChunkQueue.tryAdd(chunk.getPos(),true);
    }

}
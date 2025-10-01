package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelSnowMixin {

    @Inject(method = "setBlock", at = @At("TAIL"))
    private void ssp$trackSetBlock(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Level self = (Level) (Object) this;
        if (!(self instanceof ServerLevel level)) return;
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk instanceof ISnowTrackedChunk tracked) {
            if (newState.is(Blocks.SNOW)) {
                int layers = newState.getValue(SnowLayerBlock.LAYERS);
                tracked.sereneseasonsplus$getSnowColumns().put(pos.immutable(), layers);
            } else if (newState.is(Blocks.SNOW_BLOCK)) {
                tracked.sereneseasonsplus$getSnowColumns().put(pos.immutable(), 8);
            } else {
                tracked.sereneseasonsplus$getSnowColumns().remove(pos);
            }
        }
    }
}


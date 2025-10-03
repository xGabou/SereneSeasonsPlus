package com.Gabou.sereneseasonsplus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
        com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature.accumulateColumnUpdate(pos, newState);
    }
}

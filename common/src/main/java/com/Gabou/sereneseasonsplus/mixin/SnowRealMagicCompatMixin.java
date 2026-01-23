package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.logic.SnowChunkWeatherLogic;
import com.Gabou.sereneseasonsplus.util.IServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


//@Mixin(value = WorldTickHandler.class, remap = false)
public class SnowRealMagicCompatMixin {

//    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
//    private static void sereneseasonsplus$injectTick(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
//        if(!CommonSnowBlockFeature.isSnowFeatureEnabled())
//            return;
//        if(level instanceof IServerLevel serverLevel) {
//            SnowChunkWeatherLogic.run(level, serverLevel.sereneseasonsplus$getChunkLevel());
//            cir.cancel();
//        }
//    }

}

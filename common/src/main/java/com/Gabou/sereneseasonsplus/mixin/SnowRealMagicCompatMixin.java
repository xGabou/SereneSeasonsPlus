package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "snownee.snow.WorldTickHandler", remap = false)
public class SnowRealMagicCompatMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private static void sereneseasonsplus$injectTick(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(!CommonSnowBlockFeature.isSnowFeatureEnabled())
            return;
    }

}

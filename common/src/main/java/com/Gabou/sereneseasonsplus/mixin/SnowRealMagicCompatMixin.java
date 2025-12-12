package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.features.CommonSnowBlockFeature;
import com.Gabou.sereneseasonsplus.features.logic.SnowChunkWeatherLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.snow.WorldTickHandler;
import snownee.snow.util.CommonProxy;

import java.lang.reflect.Field;

@Mixin(value = WorldTickHandler.class, remap = false)
public class SnowRealMagicCompatMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void sereneseasonsplus$injectTick(ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        if(!CommonSnowBlockFeature.isSnowFeatureEnabled())
            return;
        SnowChunkWeatherLogic.run(level, chunk);
        ci.cancel();
    }

}

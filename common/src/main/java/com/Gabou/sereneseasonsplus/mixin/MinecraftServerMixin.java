package com.Gabou.sereneseasonsplus.mixin;
import com.Gabou.sereneseasonsplus.access.MinecraftServerAccess;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerAccess {
    @Unique
    private boolean snow$lastHaveTime;

    @Inject(method = "haveTime", at = @At("RETURN"))
    private void snow$captureHaveTime(CallbackInfoReturnable<Boolean> cir) {
        this.snow$lastHaveTime = cir.getReturnValue();
    }

    @Unique
    public boolean sereneseasonsplus$tempsEcoule() {
        return this.snow$lastHaveTime;
    }
}

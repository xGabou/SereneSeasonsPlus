package com.Gabou.sereneseasonsplus.mixin;

import betterdays.wrappers.ServerLevelWrapper;
import com.Gabou.sereneseasonsplus.config.SereneExtendedConfig;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies SSP's configurable wake time after Better Days finishes sleep. */
@Pseudo
@Mixin(targets = "betterdays.time.TimeService", remap = false)
public abstract class BetterDaysSleepWakeTimeMixin {
    @Shadow(remap = false)
    public ServerLevelWrapper level;

    @Inject(
            method = "handleMorning",
            at = @At(
                    value = "INVOKE",
                    target = "Lbetterdays/platform/services/IPlatform;onSleepFinished(Lbetterdays/wrappers/ServerLevelWrapper;J)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = false
    )
    private void sereneSeasonsPlus$setBetterDaysWakeTime(CallbackInfo ci) {
        if (!SereneExtendedConfig.ENABLE_BETTER_DAYS_SLEEP_WAKE_TIME_FIX.get()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level.get();
        long currentTime = serverLevel.getDayTime();
        long startOfDay = Math.floorDiv(currentTime, 24000L) * 24000L;
        long wakeTime = startOfDay + SereneExtendedConfig.BETTER_DAYS_SLEEP_WAKE_TIME.get();
        if (wakeTime <= currentTime) {
            wakeTime += 24000L;
        }
        serverLevel.setDayTime(wakeTime);
    }
}

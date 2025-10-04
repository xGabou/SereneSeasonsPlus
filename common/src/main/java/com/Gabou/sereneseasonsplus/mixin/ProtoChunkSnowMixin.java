package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ProtoChunk.class)
public class ProtoChunkSnowMixin implements ISnowTrackedChunk {
    @Unique
    private int sereneseasonsplus$lastWinterId = -1;

    @Unique
    private final Map<BlockPos, Integer> sereneseasonsplus$snowColumns = new HashMap<>();
    @Unique
    private final Set<BlockPos> sereneseasonsplus$iceColumns = new HashSet<>();

    @Unique
    private int sereneseasonsplus$surfaceHeight = -1;

    @Unique
    private float sereneseasonsplus$stormProgress = 0f;
    @Unique
    private int sereneseasonsplus$stormIdApplied = 0;
    @Unique
    private int sereneseasonsplus$lastProgressTick = 0;

    @Override
    public int sereneseasonsplus$getLastWinterId() {
        return sereneseasonsplus$lastWinterId;
    }

    @Override
    public void sereneseasonsplus$setLastWinterId(int id) {
        this.sereneseasonsplus$lastWinterId = id;
    }

    @Override
    public Map<BlockPos, Integer> sereneseasonsplus$getSnowColumns() {
        return sereneseasonsplus$snowColumns;
    }

    @Override
    public Set<BlockPos> sereneseasonsplus$getIceColumns() {
        return sereneseasonsplus$iceColumns;
    }

    @Override
    public int sereneseasonsplus$getSurfaceHeight() {
        return sereneseasonsplus$surfaceHeight;
    }

    @Override
    public void sereneseasonsplus$setSurfaceHeight(int height) {
        sereneseasonsplus$surfaceHeight = height;
    }

    @Override
    public float sereneseasonsplus$getStormProgress() {
        return sereneseasonsplus$stormProgress;
    }

    @Override
    public void sereneseasonsplus$setStormProgress(float progress) {
        sereneseasonsplus$stormProgress = progress;
    }

    @Override
    public int sereneseasonsplus$getStormIdApplied() {
        return sereneseasonsplus$stormIdApplied;
    }

    @Override
    public void sereneseasonsplus$setStormIdApplied(int id) {
        sereneseasonsplus$stormIdApplied = id;
    }

    @Override
    public int sereneseasonsplus$getLastProgressTick() {
        return sereneseasonsplus$lastProgressTick;
    }

    @Override
    public void sereneseasonsplus$setLastProgressTick(int tick) {
        sereneseasonsplus$lastProgressTick = tick;
    }
}

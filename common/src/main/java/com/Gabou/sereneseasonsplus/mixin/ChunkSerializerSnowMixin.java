package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.SereneSeasonPlusCommon;
import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Mixin(SerializableChunkData.class)
public abstract class ChunkSerializerSnowMixin {
    private static final String SSP = "SereneSeasonsPlus";

    // Save
    @Inject(method = "write", at = @At("RETURN"), cancellable = false)
    private void ssp$write(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag root = cir.getReturnValue();
        if (!(this instanceof ISnowTrackedChunk tracked)) return;

        CompoundTag tag = new CompoundTag();
        tag.putInt("LastWinterId", tracked.sereneseasonsplus$getLastWinterId());
        tag.putInt("SurfaceHeight", tracked.sereneseasonsplus$getSurfaceHeight());
        tag.putFloat("StormProgress", tracked.sereneseasonsplus$getStormProgress());
        tag.putInt("StormIdApplied", tracked.sereneseasonsplus$getStormIdApplied());
        tag.putInt("LastProgressTick", tracked.sereneseasonsplus$getLastProgressTick());
        tag.putInt("AvailableColumns", tracked.sereneseasonsplus$getAvailableSnowColumns());
        tag.putInt("DestroyedStormId", tracked.sereneseasonsplus$getDestroyedStormId());

        // Snow columns
        ListTag snowList = new ListTag();
        for (var e : tracked.sereneseasonsplus$getSnowColumns().entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", e.getKey().asLong());
            entry.putInt("Layers", e.getValue());
            snowList.add(entry);
        }
        tag.put("SnowColumns", snowList);

        // Ice columns
        ListTag iceList = new ListTag();
        for (var pos : tracked.sereneseasonsplus$getIceColumns()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            iceList.add(entry);
        }
        tag.put("IceColumns", iceList);

        // Destroyed columns
        ListTag destroyedList = new ListTag();
        for (Long key : tracked.sereneseasonsplus$getDestroyedColumns()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("XZ", key);
            destroyedList.add(entry);
        }
        tag.put("DestroyedColumns", destroyedList);

        // Add all of it under your namespace
        root.put("SereneSeasonsPlus", tag);
    }

    @Unique
    private static final Map<ChunkPos, CompoundTag> SS_PLUS_CACHE = new HashMap<>();

    @Inject(method = "parse", at = @At("RETURN"))
    private static void ssp$cacheTag(LevelHeightAccessor level, RegistryAccess access, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir) {
        if (nbt.contains("SereneSeasonsPlus")) {
            ChunkPos pos = new ChunkPos(nbt.getInt("xPos").get(), nbt.getInt("zPos").get());
            SS_PLUS_CACHE.put(pos, nbt.getCompound("SereneSeasonsPlus").get().copy());
        }
    }

    // Load
    @Inject(method = "read", at = @At("RETURN"))
    private void ssp$restore(ServerLevel level, PoiManager poi, RegionStorageInfo info, ChunkPos chunkPos, CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk chunk = cir.getReturnValue();
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return;
        CompoundTag tag = SS_PLUS_CACHE.remove(chunkPos);
        if (tag == null || tag.isEmpty()) return;

        // Base fields
        if (tag.contains("LastWinterId")) tracked.sereneseasonsplus$setLastWinterId(tag.getInt("LastWinterId").get());
        if (tag.contains("SurfaceHeight"))
            tracked.sereneseasonsplus$setSurfaceHeight(tag.getInt("SurfaceHeight").get());
        if (tag.contains("StormProgress"))
            tracked.sereneseasonsplus$setStormProgress(tag.getFloat("StormProgress").get());
        if (tag.contains("StormIdApplied"))
            tracked.sereneseasonsplus$setStormIdApplied(tag.getInt("StormIdApplied").get());
        if (tag.contains("LastProgressTick"))
            tracked.sereneseasonsplus$setLastProgressTick(tag.getInt("LastProgressTick").get());
        if (tag.contains("AvailableColumns"))
            tracked.sereneseasonsplus$setAvailableSnowColumns(tag.getInt("AvailableColumns").get());
        if (tag.contains("DestroyedStormId"))
            tracked.sereneseasonsplus$setDestroyedStormId(tag.getInt("DestroyedStormId").get());

        // Snow columns
        tracked.sereneseasonsplus$getSnowColumns().clear();
        ListTag snowList = tag.getList("SnowColumns").get();
        for (int i = 0; i < snowList.size(); i++) {
            CompoundTag entry = snowList.getCompound(i).get();
            BlockPos pos = BlockPos.of(entry.getLong("Pos").get());
            int layers = entry.getInt("Layers").get();
            tracked.sereneseasonsplus$getSnowColumns().put(pos.immutable(), layers);
        }

        // Ice columns
        tracked.sereneseasonsplus$getIceColumns().clear();
        ListTag iceList = tag.getList("IceColumns").get();
        for (int i = 0; i < iceList.size(); i++) {
            CompoundTag entry = iceList.getCompound(i).get();
            BlockPos pos = BlockPos.of(entry.getLong("Pos").get());
            tracked.sereneseasonsplus$getIceColumns().add(pos.immutable());
        }

        // Destroyed columns
        tracked.sereneseasonsplus$getDestroyedColumns().clear();
        ListTag destroyedList = tag.getList("DestroyedColumns").get();
        for (int i = 0; i < destroyedList.size(); i++) {
            CompoundTag entry = destroyedList.getCompound(i).get();
            tracked.sereneseasonsplus$getDestroyedColumns().add(entry.getLong("XZ").get());
        }
    }
}


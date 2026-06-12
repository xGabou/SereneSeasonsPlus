package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkSerializer.class)
public abstract class ChunkSerializerSnowMixin {
    private static final String SSP = "SereneSeasonsPlus";

    // Save
    @Inject(method = "write", at = @At("RETURN"))
    private static void ssp$write(ServerLevel level, ChunkAccess access, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag root = cir.getReturnValue(); // This is the $$3 tag in the decompiled code
        if (!(access instanceof ISnowTrackedChunk tracked)) return;

        CompoundTag tag = new CompoundTag();
        tag.putInt("LastWinterId", tracked.sereneseasonsplus$getLastWinterId());
        tag.putInt("SurfaceHeight", tracked.sereneseasonsplus$getSurfaceHeight());
        tag.putFloat("StormProgress", tracked.sereneseasonsplus$getStormProgress());
        tag.putInt("StormIdApplied", tracked.sereneseasonsplus$getStormIdApplied());
        tag.putInt("LastProgressTick", tracked.sereneseasonsplus$getLastProgressTick());
        tag.putInt("AvailableColumns", tracked.sereneseasonsplus$getAvailableSnowColumns());
        tag.putInt("DestroyedStormId", tracked.sereneseasonsplus$getDestroyedStormId());
        tag.putInt("SnowSyncGeneration", tracked.sereneseasonsplus$getSnowSyncGeneration());
        tag.putInt("AppliedStormCount", tracked.sereneseasonsplus$getAppliedStormCount());

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

        // Destroyed columns (per-storm, xz-packed longs)
        ListTag destroyedList = new ListTag();
        for (Long key : tracked.sereneseasonsplus$getDestroyedColumns()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("XZ", key);
            destroyedList.add(entry);
        }
        tag.put("DestroyedColumns", destroyedList);

        // Add all of it under your namespace
        root.put("SereneSeasonsPlus", tag);
        //LoggerFactory.getLogger("ChunkSeriliazer").info("[SS+] Writing snow data for {}", access.getPos());


        // No setReturnValue — you’re modifying the actual return tag
    }

    @Inject(
            method = "read(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/ai/village/poi/PoiManager;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/chunk/ProtoChunk;",
            at = @At("RETURN")
    )
    private static void ssp$rehydrateOnRead(ServerLevel level,
                                            PoiManager poi,
                                            ChunkPos pos,
                                            CompoundTag nbt,
                                            CallbackInfoReturnable<ProtoChunk> cir) {
        CompoundTag tag = nbt.getCompound(SSP);
        if (tag.isEmpty()) return;

        ProtoChunk ret = cir.getReturnValue();

        // If this was a fully-generated chunk, read() returned an ImposterProtoChunk wrapping a LevelChunk.
        if (ret instanceof net.minecraft.world.level.chunk.ImposterProtoChunk ipc) {
            LevelChunk lc = (LevelChunk) (Object) ipc.getWrapped(); // method name is getWrapped() in 1.20.1
            ssp$applyTagToTracked(lc, tag);
        } else {
            // Otherwise it's a real ProtoChunk (during worldgen)
            ssp$applyTagToTracked(ret, tag);
        }
    }

    @Unique
    private static void ssp$applyTagToTracked(Object chunk, CompoundTag tag) {
        if (!(chunk instanceof ISnowTrackedChunk tracked)) return;

        if (tag.contains("LastWinterId")) tracked.sereneseasonsplus$setLastWinterId(tag.getInt("LastWinterId"));
        if (tag.contains("SurfaceHeight")) tracked.sereneseasonsplus$setSurfaceHeight(tag.getInt("SurfaceHeight"));
        if (tag.contains("StormProgress")) tracked.sereneseasonsplus$setStormProgress(tag.getFloat("StormProgress"));
        if (tag.contains("StormIdApplied")) tracked.sereneseasonsplus$setStormIdApplied(tag.getInt("StormIdApplied"));
        if (tag.contains("LastProgressTick")) tracked.sereneseasonsplus$setLastProgressTick(tag.getInt("LastProgressTick"));
        if (tag.contains("AvailableColumns")) tracked.sereneseasonsplus$setAvailableSnowColumns(tag.getInt("AvailableColumns"));
        if (tag.contains("DestroyedStormId")) tracked.sereneseasonsplus$setDestroyedStormId(tag.getInt("DestroyedStormId"));
        if (tag.contains("SnowSyncGeneration")) tracked.sereneseasonsplus$setSnowSyncGeneration(tag.getInt("SnowSyncGeneration"));
        if (tag.contains("AppliedStormCount")) tracked.sereneseasonsplus$setAppliedStormCount(tag.getInt("AppliedStormCount"));

        // Snow columns
        tracked.sereneseasonsplus$getSnowColumns().clear();
        ListTag snow = tag.getList("SnowColumns", 10);
        for (int i = 0; i < snow.size(); i++) {
            CompoundTag e = snow.getCompound(i);
            tracked.sereneseasonsplus$getSnowColumns().put(
                    net.minecraft.core.BlockPos.of(e.getLong("Pos")).immutable(),
                    e.getInt("Layers")
            );
        }

        // Ice columns
        tracked.sereneseasonsplus$getIceColumns().clear();
        ListTag ice = tag.getList("IceColumns", 10);
        for (int i = 0; i < ice.size(); i++) {
            CompoundTag e = ice.getCompound(i);
            tracked.sereneseasonsplus$getIceColumns().add(
                    net.minecraft.core.BlockPos.of(e.getLong("Pos")).immutable()
            );
        }

        // Destroyed columns
        tracked.sereneseasonsplus$getDestroyedColumns().clear();
        ListTag destroyed = tag.getList("DestroyedColumns", 10);
        for (int i = 0; i < destroyed.size(); i++) {
            CompoundTag e = destroyed.getCompound(i);
            tracked.sereneseasonsplus$getDestroyedColumns().add(e.getLong("XZ"));
        }

        // optional debug
        // org.slf4j.LoggerFactory.getLogger("SS+")
        //    .info("[SS+] Restored {} snow cols into {}", tracked.sereneseasonsplus$getSnowColumns().size(), (chunk instanceof LevelChunk lc ? lc.getPos() : "proto"));
    }

    // Load
    @Inject(method = "read", at = @At("RETURN"))
    private static void ssp$read(ServerLevel level, PoiManager poi, ChunkPos pos, CompoundTag nbt, CallbackInfoReturnable<ProtoChunk> cir) {
        ChunkAccess access = cir.getReturnValue();
        if (access instanceof ISnowTrackedChunk tracked) {
            CompoundTag tag = nbt.getCompound(SSP);
            if (!tag.isEmpty()) {
                if (tag.contains("LastWinterId")) {
                    tracked.sereneseasonsplus$setLastWinterId(tag.getInt("LastWinterId"));
                }
                if (tag.contains("SurfaceHeight")) {
                    tracked.sereneseasonsplus$setSurfaceHeight(tag.getInt("SurfaceHeight"));
                }

                // Load storm progress tracking
                if (tag.contains("StormProgress")) {
                    tracked.sereneseasonsplus$setStormProgress(tag.getFloat("StormProgress"));
                }
                if (tag.contains("StormIdApplied")) {
                    tracked.sereneseasonsplus$setStormIdApplied(tag.getInt("StormIdApplied"));
                }
                if (tag.contains("LastProgressTick")) {
                    tracked.sereneseasonsplus$setLastProgressTick(tag.getInt("LastProgressTick"));
                }
                if (tag.contains("AvailableColumns")) {
                    tracked.sereneseasonsplus$setAvailableSnowColumns(tag.getInt("AvailableColumns"));
                }
                if (tag.contains("DestroyedStormId")) {
                    tracked.sereneseasonsplus$setDestroyedStormId(tag.getInt("DestroyedStormId"));
                }
                if (tag.contains("SnowSyncGeneration")) {
                    tracked.sereneseasonsplus$setSnowSyncGeneration(tag.getInt("SnowSyncGeneration"));
                }
                if (tag.contains("AppliedStormCount")) {
                    tracked.sereneseasonsplus$setAppliedStormCount(tag.getInt("AppliedStormCount"));
                }

                // Load snow columns
                tracked.sereneseasonsplus$getSnowColumns().clear();
                ListTag list = tag.getList("SnowColumns", 10); // 10 = CompoundTag id
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    long posLong = entry.getLong("Pos");
                    int layers = entry.getInt("Layers");
                    net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.of(posLong);
                    tracked.sereneseasonsplus$getSnowColumns().put(bp.immutable(), layers);
                }

                // Load ice columns
                tracked.sereneseasonsplus$getIceColumns().clear();
                ListTag iceList = tag.getList("IceColumns", 10);
                for (int i = 0; i < iceList.size(); i++) {
                    CompoundTag entry = iceList.getCompound(i);
                    long posLong = entry.getLong("Pos");
                    net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.of(posLong);
                    tracked.sereneseasonsplus$getIceColumns().add(bp.immutable());
                }

                // Load destroyed columns
                tracked.sereneseasonsplus$getDestroyedColumns().clear();
                ListTag destroyedList = tag.getList("DestroyedColumns", 10);
                for (int i = 0; i < destroyedList.size(); i++) {
                    CompoundTag entry = destroyedList.getCompound(i);
                    long xz = entry.getLong("XZ");
                    tracked.sereneseasonsplus$getDestroyedColumns().add(xz);
                }
            }
        }
    }
}

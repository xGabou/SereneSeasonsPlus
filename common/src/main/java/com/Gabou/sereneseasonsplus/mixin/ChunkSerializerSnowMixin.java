package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerSnowMixin {
    private static final String SSP = "SereneSeasonsPlus";

    // Save
    @Inject(method = "write", at = @At("RETURN"), cancellable = true)
    private static void ssp$write(ServerLevel level, ChunkAccess access, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag root = cir.getReturnValue();
        if (access instanceof ISnowTrackedChunk tracked) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("LastWinterId", tracked.sereneseasonsplus$getLastWinterId());

            // Save surface height if available
            tag.putInt("SurfaceHeight", tracked.sereneseasonsplus$getSurfaceHeight());

            // Save storm progress tracking
            tag.putFloat("StormProgress", tracked.sereneseasonsplus$getStormProgress());
            tag.putInt("StormIdApplied", tracked.sereneseasonsplus$getStormIdApplied());
            tag.putInt("LastProgressTick", tracked.sereneseasonsplus$getLastProgressTick());

            // Persist snow columns as a list of {Pos: long, Layers: int}
            ListTag list = new ListTag();
            for (java.util.Map.Entry<net.minecraft.core.BlockPos, Integer> e : tracked.sereneseasonsplus$getSnowColumns().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Pos", e.getKey().asLong());
                entry.putInt("Layers", e.getValue());
                list.add(entry);
            }
            tag.put("SnowColumns", list);

            // Persist ice columns as list of Pos: long
            ListTag iceList = new ListTag();
            for (net.minecraft.core.BlockPos p : tracked.sereneseasonsplus$getIceColumns()) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Pos", p.asLong());
                iceList.add(entry);
            }
            tag.put("IceColumns", iceList);

            root.put(SSP, tag);
            cir.setReturnValue(root);
        }
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
            }
        }
    }
}

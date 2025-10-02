package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ISnowTrackedChunk;
import net.minecraft.nbt.CompoundTag;
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

            // Persist snow columns as a list of {Pos: long, Layers: int}
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (java.util.Map.Entry<net.minecraft.core.BlockPos, Integer> e : tracked.sereneseasonsplus$getSnowColumns().entrySet()) {
                net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
                entry.putLong("Pos", e.getKey().asLong());
                entry.putInt("Layers", e.getValue());
                list.add(entry);
            }
            tag.put("SnowColumns", list);

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
                // Load snow columns
                tracked.sereneseasonsplus$getSnowColumns().clear();
                net.minecraft.nbt.ListTag list = tag.getList("SnowColumns", 10); // 10 = CompoundTag id
                for (int i = 0; i < list.size(); i++) {
                    net.minecraft.nbt.CompoundTag entry = list.getCompound(i);
                    long posLong = entry.getLong("Pos");
                    int layers = entry.getInt("Layers");
                    net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.of(posLong);
                    tracked.sereneseasonsplus$getSnowColumns().put(bp.immutable(), layers);
                }
            }
        }

    }

    }


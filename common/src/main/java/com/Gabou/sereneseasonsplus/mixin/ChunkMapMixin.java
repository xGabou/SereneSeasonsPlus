package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.util.ChunkMapInterfaceAccess;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapInterfaceAccess {

    // shadow the private field
    @Shadow
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    /**
     * Public bridge method for snow logic.
     * Returns the same thing getChunks() does,
     * but without using an accessor.
     */
    @Unique
    public Iterable<ChunkHolder> snow$getChunksSafe() {
        return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
    }
}

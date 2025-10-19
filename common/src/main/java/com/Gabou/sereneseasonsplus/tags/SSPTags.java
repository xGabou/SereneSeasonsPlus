package com.Gabou.sereneseasonsplus.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class SSPTags {
    public static class Blocks {
        public static final TagKey<Block> MELTABLE =
                TagKey.create(Registries.BLOCK, new ResourceLocation("sereneseasonsplus", "meltable"));
        public static final TagKey<Block> FLOWERS =
                TagKey.create(Registries.BLOCK,  new ResourceLocation("sereneseasonsplus", "flowers"));
    }
}


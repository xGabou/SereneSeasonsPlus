package com.Gabou.sereneseasonsplus.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class SSPTags {
    public static class Blocks {
        public static final TagKey<Block> MELTABLE =
                TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("sereneseasonsplus", "meltable"));
        public static final TagKey<Block> FLOWERS =
                TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("sereneseasonsplus", "flowers"));
        public static final TagKey<Block> SNOW_REPLACEABLE =
                TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("sereneseasonsplus", "snow_replaceable"));
    }
}


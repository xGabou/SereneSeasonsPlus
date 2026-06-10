package net.Gabou.projectatmosphere.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record BiomeInstanceKey(ResourceLocation biomeId, BlockPos pos) {
}

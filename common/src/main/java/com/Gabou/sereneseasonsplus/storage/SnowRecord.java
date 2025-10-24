package com.Gabou.sereneseasonsplus.storage;

import net.minecraft.nbt.CompoundTag;

public class SnowRecord {
    public float minLayers;
    public float avgLayers;
    public float maxLayers;
    public int[] distributionPattern; // optional; null if absent

    public SnowRecord() {}

    public SnowRecord(float minLayers, float avgLayers, float maxLayers, int[] distributionPattern) {
        this.minLayers = minLayers;
        this.avgLayers = avgLayers;
        this.maxLayers = maxLayers;
        this.distributionPattern = distributionPattern;
    }

    public static SnowRecord fromTag(CompoundTag tag) {
        SnowRecord r = new SnowRecord();
        r.minLayers = tag.getFloat("MinLayers").get();
        r.avgLayers = tag.getFloat("AvgLayers").get();
        r.maxLayers = tag.getFloat("MaxLayers").get();
        if (tag.contains("DistributionPattern")) {
            r.distributionPattern = tag.getIntArray("DistributionPattern").get();
        }
        return r;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("MinLayers", minLayers);
        tag.putFloat("AvgLayers", avgLayers);
        tag.putFloat("MaxLayers", maxLayers);
        if (distributionPattern != null) {
            tag.putIntArray("DistributionPattern", distributionPattern);
        }
        return tag;
    }
}

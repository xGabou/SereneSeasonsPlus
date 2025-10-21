package com.Gabou.sereneseasonsplus.util;

import com.Gabou.sereneseasonsplus.storage.SnowRecord;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SnowGenerator {
    public static SnowRecord generateStormRecord(RandomSource random) {
        // Reduce per-storm accumulation: pick a narrow, low range
        // Previously could add up to ~6 extra layers per storm; now target 1–3 total
        float min = 1+random.nextInt(2); // at least 1 layer in affected spots
        float max = min + random.nextInt(5); // 1–3 layers total
        float avg = Mth.ceil((min + max) / 2f);

        int[] distribution = new int[16];
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = (int) (min + random.nextInt((int)(max - min) + 1));
        }
        return new SnowRecord(min, avg, max, distribution);
    }
}

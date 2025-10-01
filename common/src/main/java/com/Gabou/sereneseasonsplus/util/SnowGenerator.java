package com.Gabou.sereneseasonsplus.util;

import net.minecraft.util.RandomSource;
import com.Gabou.sereneseasonsplus.storage.SnowRecord;

public class SnowGenerator {
    public static SnowRecord generateStormRecord(RandomSource random) {
        float min = 1 + random.nextInt(2); // 1–2 layers
        float max = min + 2 + random.nextInt(5); // +2–6
        float avg = (min + max) / 2f;
        int[] distribution = new int[16];
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = (int) (min + random.nextInt((int)(max - min) + 1));
        }
        return new SnowRecord(min, avg, max, distribution);
    }
}

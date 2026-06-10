package com.Gabou.sereneseasonsplus.features;

import com.Gabou.sereneseasonsplus.storage.SnowHistorySavedData;
import net.Gabou.gaboulibs.storage.SnowRecord;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public final class SnowHistoryQueryService {
    public int computeGlobalAvg(ServerLevel level) {
        SnowHistorySavedData savedData = SnowHistorySavedData.get();
        if (savedData == null || savedData.snowHistory.isEmpty()) {
            return 0;
        }

        int excludeId = savedData.currentStormId;
        float total = 0f;
        for (Map.Entry<Integer, SnowRecord> entry : savedData.snowHistory.entrySet()) {
            if (excludeId > 0 && entry.getKey() == excludeId) {
                continue;
            }
            total += Math.max(0f, entry.getValue().avgLayers);
        }
        return Math.round(total);
    }

    public int computeGlobalMinSum(ServerLevel level) {
        SnowHistorySavedData savedData = SnowHistorySavedData.get();
        if (savedData == null || savedData.snowHistory.isEmpty()) {
            return 0;
        }

        int excludeId = savedData.currentStormId;
        float sumMin = 0f;
        for (Map.Entry<Integer, SnowRecord> entry : savedData.snowHistory.entrySet()) {
            if (excludeId > 0 && entry.getKey() == excludeId) {
                continue;
            }
            sumMin += Math.max(0f, entry.getValue().minLayers);
        }
        return Math.max(0, Math.round(sumMin));
    }

    public SnowRecord aggregateFinishedStormSums(ServerLevel level) {
        SnowHistorySavedData savedData = SnowHistorySavedData.get();
        if (savedData == null || savedData.snowHistory.isEmpty()) {
            return null;
        }

        int excludeId = savedData.currentStormId;
        float sumMin = 0f;
        float sumAvg = 0f;
        float sumMax = 0f;
        int count = 0;
        for (Map.Entry<Integer, SnowRecord> entry : savedData.snowHistory.entrySet()) {
            if (excludeId > 0 && entry.getKey() == excludeId) {
                continue;
            }

            SnowRecord record = entry.getValue();
            sumMin += Math.max(0f, record.minLayers);
            sumAvg += Math.max(0f, record.avgLayers);
            sumMax += Math.max(0f, record.maxLayers);
            count++;
        }

        if (count == 0) {
            return null;
        }
        return new SnowRecord(sumMin, sumAvg, sumMax, null);
    }
}

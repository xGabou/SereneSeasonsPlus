package com.Gabou.sereneseasonsplus.util;

import sereneseasons.api.season.Season;

public interface ISnowTrackedChunk {
    Season.SubSeason sereneseasonsplus$getLastSeason();
    void sereneseasonsplus$setLastSeason(Season.SubSeason season);

    boolean sereneseasonsplus$wasRaining();
    void sereneseasonsplus$incrementWasRaining(boolean raining);

    // Tracks whether this chunk already received its single noisy layer for the current storm
    boolean sereneseasonsplus$hasReceivedSnowLayerThisStorm();
    void sereneseasonsplus$setHasReceivedSnowLayerThisStorm(boolean value);

    // Tracks whether the chunk should receive deep-winter initialization snow
    boolean sereneseasonsplus$shouldApplyInitialSnow();
    void sereneseasonsplus$setShouldApplyInitialSnow(boolean value);

    // Remembers if deep-winter initialization snow has already been applied
    boolean sereneseasonsplus$hasAppliedInitialSnow();
    void sereneseasonsplus$setHasAppliedInitialSnow(boolean value);
    int sereneseasonsplus$getSnowCount();
    void sereneseasonsplus$incrementSnowCount();

    void sereneseasonsplus$setSnowCount(int value);


    void sereneseasonsplus$willReceiveSnow(boolean b);
    boolean sereneseasonsplus$shouldReceiveSnow();
    int sereneseasonsplus$getLastWinterId();
    void sereneseasonsplus$setLastWinterId(int id);

    // Per-chunk snow columns: maps a block position to its current snow layer count
    java.util.Map<net.minecraft.core.BlockPos, Integer> sereneseasonsplus$getSnowColumns();
    default int sereneseasonsplus$getSnowColumnsTotalLayers() {
        int sum = 0;
        for (int v : sereneseasonsplus$getSnowColumns().values()) sum += v;
        return sum;
    }
    default int sereneseasonsplus$getSnowColumnsCount() {
        return sereneseasonsplus$getSnowColumns().size();
    }

}


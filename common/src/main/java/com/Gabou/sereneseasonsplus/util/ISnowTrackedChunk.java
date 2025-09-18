package com.Gabou.sereneseasonsplus.util;

import sereneseasons.api.season.Season;

public interface ISnowTrackedChunk {
    boolean sereneseasonsplus$needsSnowUpdate();
    void sereneseasonsplus$setNeedsSnowUpdate(boolean needsUpdate);

    Season.SubSeason sereneseasonsplus$getLastSeason();
    void sereneseasonsplus$setLastSeason(Season.SubSeason season);

    boolean sereneseasonsplus$wasRaining();
    void sereneseasonsplus$setWasRaining(boolean raining);
}


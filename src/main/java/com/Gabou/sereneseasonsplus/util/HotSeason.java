package com.Gabou.sereneseasonsplus.util;

import sereneseasons.api.season.Season;

public enum HotSeason {
    EARLY_SUMMER(Season.SubSeason.EARLY_SUMMER),
    MID_SUMMER(Season.SubSeason.MID_SUMMER),
    LATE_SUMMER(Season.SubSeason.LATE_SUMMER),
    LATE_SPRING(Season.SubSeason.LATE_SPRING),
    EARLY_AUTUMN(Season.SubSeason.EARLY_AUTUMN);

    private final Season.SubSeason boundSubSeason;

    HotSeason(Season.SubSeason boundSubSeason) {
        this.boundSubSeason = boundSubSeason;
    }

    /**
     * TODO: describe method.
     * @return description
     */
    public Season.SubSeason getSubSeason() {
        return boundSubSeason;
    }

    /**
     * TODO: describe method.
     *
     * @param sub description
     * @return description
     */
    public static HotSeason fromSubSeason(Season.SubSeason sub) {
        for (HotSeason hs : values()) {
            if (hs.boundSubSeason == sub) {
                return hs;
            }
        }
        return null; 
    }

    /**
     * TODO: describe method.
     *
     * @param sub description
     * @return description
     */
    public static boolean isHotSeason(Season.SubSeason sub) {
        return fromSubSeason(sub) != null;
    }
}

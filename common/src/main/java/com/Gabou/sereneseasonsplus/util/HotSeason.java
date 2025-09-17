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
     * Returns the Serene Seasons sub-season represented by this enum value.
     *
     * @return bound sub-season
     */
    public Season.SubSeason getSubSeason() {
        return boundSubSeason;
    }

    /**
     * Maps a sub-season to its HotSeason enum value if it is considered hot.
     *
     * @param sub sub-season to test
     * @return matching HotSeason value, or null if not hot
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
     * Convenience predicate: whether a sub-season is considered a hot season.
     *
     * @param sub sub-season to test
     * @return true if the sub-season is a hot season
     */
    public static boolean isHotSeason(Season.SubSeason sub) {
        return fromSubSeason(sub) != null;
    }
}

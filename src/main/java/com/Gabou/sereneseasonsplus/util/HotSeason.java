package com.Gabou.sereneseasonsplus.util;

import sereneseasons.api.season.Season;

public enum HotSeason {
    EARLY_SUMMER(Season.SubSeason.EARLY_SUMMER),
    MID_SUMMER(Season.SubSeason.MID_SUMMER),
    LATE_SUMMER(Season.SubSeason.LATE_SUMMER),
    LATE_SPRING(Season.SubSeason.LATE_SPRING),
    EARLY_AUTUMN(Season.SubSeason.EARLY_AUTUMN);

    private final Season.SubSeason boundSubSeason;

    /**
     * Binds this enum constant to a specific sub-season.
     *
     * @param boundSubSeason the sub-season represented
     */
    HotSeason(Season.SubSeason boundSubSeason) {
        this.boundSubSeason = boundSubSeason;
    }

    /**
     * Returns the sub-season associated with this enum constant.
     *
     * @return bound sub-season
     */
    public Season.SubSeason getSubSeason() {
        return boundSubSeason;
    }

    /**
     * Resolves a {@code HotSeason} from the given sub-season.
     *
     * @param sub a sub-season
     * @return matching {@code HotSeason} or {@code null} if none
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
     * Tests whether the provided sub-season is considered hot.
     *
     * @param sub a sub-season
     * @return true if the sub-season is hot
     */
    public static boolean isHotSeason(Season.SubSeason sub) {
        return fromSubSeason(sub) != null;
    }
}

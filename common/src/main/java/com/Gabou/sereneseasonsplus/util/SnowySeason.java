package com.Gabou.sereneseasonsplus.util;

import sereneseasons.api.season.Season;

public enum SnowySeason {
    EARLY_WINTER(Season.SubSeason.EARLY_WINTER),
    MID_WINTER(Season.SubSeason.MID_WINTER),
    LATE_WINTER(Season.SubSeason.LATE_WINTER),
    EARLY_SPRING(Season.SubSeason.EARLY_SPRING);

    private final Season.SubSeason boundSubSeason;

    SnowySeason(Season.SubSeason boundSubSeason) {
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
     * Maps a sub-season to its SnowySeasons enum value if it is considered snowy.
     *
     * @param sub sub-season to test
     * @return matching SnowySeasons value, or null if not snowy
     */
    public static SnowySeason fromSubSeason(Season.SubSeason sub) {
        for (SnowySeason ss : values()) {
            if (ss.boundSubSeason == sub) {
                return ss;
            }
        }
        return null;
    }

    /**
     * Convenience predicate: whether a sub-season is considered a snowy season.
     *
     * @param sub sub-season to test
     * @return true if the sub-season is a snowy season
     */
    public static boolean isSnowySeason(Season.SubSeason sub) {
        return fromSubSeason(sub) != null;
    }
}

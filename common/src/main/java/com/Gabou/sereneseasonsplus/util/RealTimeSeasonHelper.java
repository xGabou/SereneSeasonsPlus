package com.Gabou.sereneseasonsplus.util;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sereneseasons.api.season.Season;
import sereneseasons.season.SeasonHandler;
import sereneseasons.season.SeasonSavedData;
import sereneseasons.season.SeasonTime;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

/**
 * Keeps Serene Seasons aligned with real-world Canadian months.
 */
public final class RealTimeSeasonHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("RealTimeCanadianSeason");
    private static final ZoneId CANADA_EASTERN = ZoneId.of("America/Toronto");

    private static LocalDate lastAppliedDate = null;
    private static Season.SubSeason lastAppliedTarget = null;

    private RealTimeSeasonHelper() {
    }

    /**
     * Synchronize the season to the current real-world date if enabled.
     *
     * @param level   overworld level
     * @param enabled config flag
     */
    public static void sync(ServerLevel level, boolean enabled) {
        if (!enabled || level == null || level.isClientSide()) {
            return;
        }

        SeasonSavedData data = SeasonHandler.getSeasonSavedData(level);
        if (data == null) {
            return;
        }

        LocalDate today = LocalDate.now(CANADA_EASTERN);
        Season.SubSeason target = mapDateToSubSeason(today);
        if (target == null) {
            return;
        }

        SeasonTime current = new SeasonTime(data.seasonCycleTicks);
        Season.SubSeason currentSubSeason = current.getSubSeason();

        // Only intervene when the date advances or SS drifts into a different sub-season.
        boolean needsUpdate = currentSubSeason != target || !today.equals(lastAppliedDate) || target != lastAppliedTarget;
        if (!needsUpdate) {
            return;
        }

        int ticksPerSubSeason = current.getSubSeasonDuration();
        int cycleDuration = current.getCycleDuration();
        if (ticksPerSubSeason <= 0 || cycleDuration <= 0) {
            return;
        }

        int targetIndex = indexOf(target);
        if (targetIndex < 0) {
            return;
        }

        int desiredTicks = (int) (((long) targetIndex * ticksPerSubSeason) % cycleDuration);
        if (data.seasonCycleTicks != desiredTicks || currentSubSeason != target) {
            data.seasonCycleTicks = desiredTicks;
            data.setDirty();
            SeasonHandler.sendSeasonUpdate(level);
            LOGGER.info("Synced Serene Seasons to {} for {}", target, today);
        }

        lastAppliedTarget = target;
        lastAppliedDate = today;
    }

    private static int indexOf(Season.SubSeason target) {
        Season.SubSeason[] values = Season.SubSeason.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private static Season.SubSeason mapDateToSubSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();
        return switch (month) {
            case JANUARY -> Season.SubSeason.MID_WINTER;
            case FEBRUARY -> Season.SubSeason.LATE_WINTER;
            case MARCH -> Season.SubSeason.EARLY_SPRING;
            case APRIL -> Season.SubSeason.MID_SPRING;
            case MAY -> Season.SubSeason.LATE_SPRING;
            case JUNE -> Season.SubSeason.EARLY_SUMMER;
            case JULY -> Season.SubSeason.MID_SUMMER;
            case AUGUST -> Season.SubSeason.LATE_SUMMER;
            case SEPTEMBER -> Season.SubSeason.EARLY_AUTUMN;
            case OCTOBER -> (day <= 15 ? Season.SubSeason.MID_AUTUMN : Season.SubSeason.LATE_AUTUMN);
            case NOVEMBER -> (day <= 7 ? Season.SubSeason.LATE_AUTUMN : Season.SubSeason.EARLY_WINTER);
            case DECEMBER -> Season.SubSeason.EARLY_WINTER;
            default -> null;
        };
    }
}

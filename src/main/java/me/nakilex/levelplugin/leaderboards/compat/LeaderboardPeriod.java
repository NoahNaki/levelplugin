package me.nakilex.levelplugin.leaderboards.compat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public enum LeaderboardPeriod {
    ALLTIME,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static LeaderboardPeriod parse(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ALLTIME;
        }
    }

    public long bucketStart(long nowMillis, ZoneId zone, DayOfWeek weekStart) {
        if (this == ALLTIME) return 0L;
        ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(zone);
        ZonedDateTime start = switch (this) {
            case HOURLY -> now.withMinute(0).withSecond(0).withNano(0);
            case DAILY -> now.toLocalDate().atStartOfDay(zone);
            case WEEKLY -> {
                LocalDate date = now.toLocalDate().with(TemporalAdjusters.previousOrSame(weekStart));
                yield date.atStartOfDay(zone);
            }
            case MONTHLY -> now.withDayOfMonth(1).toLocalDate().atStartOfDay(zone);
            case YEARLY -> now.withDayOfYear(1).toLocalDate().atStartOfDay(zone);
            case ALLTIME -> throw new IllegalStateException("Handled above");
        };
        return start.toInstant().toEpochMilli();
    }
}

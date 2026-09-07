package me.nakilex.levelplugin.leaderboards.compat;

import java.math.BigDecimal;

public record LeaderboardDefinition(
        String id,
        String placeholder,
        ValueType valueType,
        boolean reverseSort,
        boolean excludeZero,
        BigDecimal scale
) {
    public enum ValueType {
        NUMBER,
        INTEGER,
        TIME_SECONDS
    }
}

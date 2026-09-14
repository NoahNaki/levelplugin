package me.nakilex.levelplugin.animatedlb;

import java.util.UUID;

public record LeaderboardEntry(UUID playerId, String name, double primaryValue, double secondaryValue) {
}

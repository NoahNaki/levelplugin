package me.nakilex.levelplugin.animatedlb;

import java.util.List;

public interface LeaderboardDataProvider {
    List<LeaderboardEntry> getEntries(BoardType type, int limit);
}

package me.nakilex.levelplugin.animatedlb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MockLeaderboardDataProvider implements LeaderboardDataProvider {
    @Override
    public List<LeaderboardEntry> getEntries(BoardType type, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            double value = switch (type) {
                case KILLS -> ThreadLocalRandom.current().nextInt(20, 700);
                case DEATHS -> ThreadLocalRandom.current().nextInt(1, 250);
                case MONEY -> ThreadLocalRandom.current().nextInt(5_000, 850_000);
            };
            entries.add(new LeaderboardEntry("Player" + (i + 1), value));
        }
        return entries;
    }
}

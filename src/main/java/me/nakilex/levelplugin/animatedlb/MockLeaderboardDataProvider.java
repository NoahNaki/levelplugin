package me.nakilex.levelplugin.animatedlb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MockLeaderboardDataProvider implements LeaderboardDataProvider {
    @Override
    public List<LeaderboardEntry> getEntries(BoardType type, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String name = "Player" + (i + 1);
            UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
            if (type == BoardType.STRONGHOLD_STAGE) {
                entries.add(new LeaderboardEntry(id, name, ThreadLocalRandom.current().nextInt(1, 30), ThreadLocalRandom.current().nextInt(1, 6)));
            } else {
                entries.add(new LeaderboardEntry(id, name, ThreadLocalRandom.current().nextInt(50, 1500), ThreadLocalRandom.current().nextInt(1, 60)));
            }
        }
        return entries;
    }
}

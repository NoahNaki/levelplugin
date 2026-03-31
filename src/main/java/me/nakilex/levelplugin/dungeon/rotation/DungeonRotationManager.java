package me.nakilex.levelplugin.dungeon.rotation;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Stateless rotation manager that deterministically resolves weekly mutators.
 */
public final class DungeonRotationManager {
    private DungeonRotationManager() {
    }

    public static Set<DungeonMutator> activeMutators(String dungeonKey) {
        int seed = rotationSeed(dungeonKey);
        DungeonMutator[] all = DungeonMutator.values();
        List<DungeonMutator> sorted = new ArrayList<>(List.of(all));
        sorted.sort(Comparator.comparingInt(m -> Math.abs((m.name() + ':' + seed).hashCode())));

        EnumSet<DungeonMutator> chosen = EnumSet.noneOf(DungeonMutator.class);
        chosen.add(sorted.get(0));
        if (sorted.size() > 1 && (seed % 3 == 0)) {
            chosen.add(sorted.get(1));
        }
        return Collections.unmodifiableSet(chosen);
    }

    public static double rewardMultiplier(String dungeonKey) {
        return activeMutators(dungeonKey).stream()
                .mapToDouble(DungeonMutator::rewardMultiplier)
                .reduce(1.0, (a, b) -> a * b);
    }

    public static double riskMultiplier(String dungeonKey) {
        return activeMutators(dungeonKey).stream()
                .mapToDouble(DungeonMutator::riskMultiplier)
                .reduce(1.0, (a, b) -> a * b);
    }

    private static int rotationSeed(String dungeonKey) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        WeekFields wf = WeekFields.of(Locale.US);
        int week = now.get(wf.weekOfWeekBasedYear());
        int year = now.getYear();
        int base = 31 * year + week;
        if (dungeonKey == null) {
            return base;
        }
        return 31 * base + dungeonKey.toLowerCase(Locale.ROOT).hashCode();
    }
}

package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public final class CommandUtil {
    private CommandUtil() {}

    public static List<String> filterStartingWith(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lower))
                .toList();
    }

    public static List<String> onlinePlayerNames(String prefix) {
        return filterStartingWith(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName).toList(), prefix);
    }

    public static List<String> simpleSuggestions(String prefix, String... options) {
        if (options == null || options.length == 0) {
            return Collections.emptyList();
        }
        return filterStartingWith(Arrays.asList(options), prefix);
    }

    public static List<String> numberOptions(String prefix, int... numbers) {
        if (numbers == null || numbers.length == 0) {
            return Collections.emptyList();
        }
        return filterStartingWith(Arrays.stream(numbers)
                .sorted()
                .mapToObj(Integer::toString)
                .toList(), prefix);
    }

    public static List<String> numberRange(String prefix, int startInclusive, int endInclusive) {
        if (endInclusive < startInclusive) {
            return Collections.emptyList();
        }
        return filterStartingWith(IntStream.rangeClosed(startInclusive, endInclusive)
                .mapToObj(Integer::toString)
                .toList(), prefix);
    }
}

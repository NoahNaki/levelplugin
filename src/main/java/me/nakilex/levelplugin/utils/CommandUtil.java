package me.nakilex.levelplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

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
}

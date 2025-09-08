package me.nakilex.levelplugin.utils.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.NakiPlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple command for parsing Naki placeholders.
 */
public class ParsePlaceholderCommand implements CommandExecutor, TabCompleter {
    private final Map<java.util.UUID, BukkitRunnable> holograms = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Usage: /" + label + " <placeholder> [holo]");
            return true;
        }
        String key = args[0];
        String parsed = PlaceholderAPI.setPlaceholders(player, "%naki_" + key + "%");
        player.sendMessage("§e" + key + "§7 = §f" + parsed);

        if (args.length >= 2 && args[1].equalsIgnoreCase("holo")) {
            BukkitRunnable existing = holograms.remove(player.getUniqueId());
            if (existing != null) existing.cancel();

            MultiLineHologram holo = new MultiLineHologram(player.getLocation().add(0, 2.0, 0));
            BukkitRunnable task = new BukkitRunnable() {
                int ticks;
                @Override
                public void run() {
                    if (!player.isOnline() || ticks++ >= 100) {
                        holo.despawn();
                        cancel();
                        holograms.remove(player.getUniqueId());
                        return;
                    }
                    String val = PlaceholderAPI.setPlaceholders(player, "%naki_" + key + "%");
                    holo.setLines(java.util.Arrays.asList(key + ":", val));
                }
            };
            holograms.put(player.getUniqueId(), task);
            task.runTaskTimer(Main.getInstance(), 0L, 20L);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            NakiPlaceholderExpansion exp = NakiPlaceholderExpansion.getInstance();
            String prefix = args[0].toLowerCase();
            if (prefix.startsWith("induel_")) {
                String namePrefix = prefix.substring("induel_".length());
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(namePrefix))
                        .map(n -> "induel_" + n)
                        .sorted()
                        .collect(Collectors.toList());
            }
            Set<String> keys = exp != null ? exp.getPlaceholderKeys() : Collections.emptySet();
            return keys.stream()
                    .filter(k -> k.startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}


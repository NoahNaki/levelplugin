package me.nakilex.levelplugin.utils.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.utils.NakiPlaceholderExpansion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple command for parsing Naki placeholders.
 */
public class ParsePlaceholderCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Usage: /" + label + " <placeholder>");
            return true;
        }
        String key = args[0];
        String parsed = PlaceholderAPI.setPlaceholders(player, "%naki_" + key + "%");
        player.sendMessage("§e" + key + "§7 = §f" + parsed);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            NakiPlaceholderExpansion exp = NakiPlaceholderExpansion.getInstance();
            Set<String> keys = exp != null ? exp.getPlaceholderKeys() : Collections.emptySet();
            String prefix = args[0].toLowerCase();
            return keys.stream()
                    .filter(k -> k.startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}


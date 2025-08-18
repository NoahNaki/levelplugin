package me.nakilex.levelplugin.pathfinding;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /pathfinding set <index>
 * /pathfinding create <name>
 * /pathfinding execute <name>
 */
public class PathfindingCommand implements CommandExecutor, TabCompleter {
    private final PathfindingManager manager;

    public PathfindingCommand(PathfindingManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /pathfinding <set|create|execute>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "set":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pathfinding set <index>");
                    return true;
                }
                try {
                    int idx = Integer.parseInt(args[1]);
                    manager.setPoint(idx, player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Point " + idx + " stored.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Index must be a number.");
                }
                return true;
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pathfinding create <name>");
                    return true;
                }
                manager.createPath(args[1]);
                player.sendMessage(ChatColor.GREEN + "Path '" + args[1] + "' created.");
                return true;
            case "execute":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pathfinding execute <name>");
                    return true;
                }
                manager.executePath(args[1]);
                player.sendMessage(ChatColor.GREEN + "Executing path '" + args[1] + "'.");
                return true;
            default:
                player.sendMessage(ChatColor.RED + "Usage: /pathfinding <set|create|execute>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of("set", "create", "execute");
            String start = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(start)) {
                    result.add(s);
                }
            }
            return result;
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set":
                    return Collections.singletonList(String.valueOf(manager.nextPointIndex()));
                case "execute":
                    List<String> names = new ArrayList<>();
                    String start = args[1].toLowerCase(Locale.ROOT);
                    for (String name : manager.getPathNames()) {
                        if (name.startsWith(start)) {
                            names.add(name);
                        }
                    }
                    return names;
                default:
                    break;
            }
        }
        return Collections.emptyList();
    }
}


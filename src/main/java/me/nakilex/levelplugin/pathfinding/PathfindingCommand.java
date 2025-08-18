package me.nakilex.levelplugin.pathfinding;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /pathfinding set <index>
 * /pathfinding create <name>
 * /pathfinding execute <name>
 */
public class PathfindingCommand implements CommandExecutor {
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
}


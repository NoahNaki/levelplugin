package me.nakilex.levelplugin.fasttravel.commands;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LocationCommand implements TabExecutor {
    private final FastTravelManager manager;

    public LocationCommand(FastTravelManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players.");
            return true;
        }
        if (args.length < 1) return false;
        String sub = args[0].toLowerCase();
        if (sub.equals("set") && args.length >= 6) {
            String name = args[1].replace('_',' ');
            ChatColor color = ChatColor.valueOf(args[2].toUpperCase());
            String desc = args[3].replace('_', ' ');
            double radius = Double.parseDouble(args[4]);
            boolean town = Boolean.parseBoolean(args[5]);
            Location loc = player.getLocation();
            manager.addLocation(name, color, desc, loc, radius, town);
            player.sendMessage(ChatColor.GREEN + "Location " + name + " added.");
        } else if (sub.equals("move") && args.length >= 2) {
            manager.moveLocation(args[1], player.getLocation());
            player.sendMessage(ChatColor.YELLOW + "Location moved.");
        } else if (sub.equals("remove") && args.length >= 2) {
            manager.removeLocation(args[1]);
            player.sendMessage(ChatColor.RED + "Location removed.");
        } else if (sub.equals("list")) {
            player.sendMessage(ChatColor.GOLD + "Fast Travel Locations:");
            manager.getPoints().forEach(pt ->
                    player.sendMessage(pt.getColor() + pt.getName() + ChatColor.GRAY + " - " + pt.getDescription())
            );
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /location set <name> <color> <description_with_underscores> <radius> <true/false>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "move", "remove", "list").stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("remove"))) {
            return manager.getPoints().stream()
                    .map(pt -> pt.getName().replace(' ', '_'))
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.stream(ChatColor.values())
                    .map(ChatColor::name)
                    .filter(color -> color.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

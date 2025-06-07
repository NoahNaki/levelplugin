package me.nakilex.levelplugin.fasttravel.commands;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LocationCommand implements CommandExecutor {
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
            String name = args[1];
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
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /location set <name> <color> <desc_with_underscores> <radius> <true/false> | move <name> | remove <name>");
        }
        return true;
    }
}

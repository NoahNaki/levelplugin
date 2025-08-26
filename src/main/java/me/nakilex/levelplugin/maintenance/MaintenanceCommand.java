package me.nakilex.levelplugin.maintenance;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MaintenanceCommand implements CommandExecutor, TabCompleter {
    private final MaintenanceManager manager;
    public MaintenanceCommand(MaintenanceManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/maintenance <on|off|add|remove|whitelist>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /maintenance on <reason>");
                    return true;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                manager.enable(reason);
                sender.sendMessage(ChatColor.GREEN + "Maintenance enabled: " + reason);
                return true;
            case "off":
                manager.disable();
                sender.sendMessage(ChatColor.GREEN + "Maintenance disabled.");
                return true;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /maintenance add <player>");
                    return true;
                }
                manager.addPlayer(args[1]);
                sender.sendMessage(ChatColor.GREEN + "Added " + args[1] + " to whitelist.");
                return true;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /maintenance remove <player>");
                    return true;
                }
                manager.removePlayer(args[1]);
                sender.sendMessage(ChatColor.GREEN + "Removed " + args[1] + " from whitelist.");
                return true;
            case "whitelist":
                Set<String> names = manager.getWhitelist();
                if (names.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "Whitelist is empty.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Whitelisted players: " + String.join(", ", names));
                }
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "/maintenance <on|off|add|remove|whitelist>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "add", "remove", "whitelist").stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return sender.getServer().getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

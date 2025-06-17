package me.nakilex.levelplugin.friend;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Command to ignore players or list ignored players. */
public class IgnoreCommand implements CommandExecutor {
    private final IgnoreManager manager;

    public IgnoreCommand(IgnoreManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        UUID id = player.getUniqueId();

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var list = manager.getIgnored(id);
            if (list.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "You are not ignoring anyone.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Ignored players:");
                for (UUID u : list) {
                    String n = Bukkit.getOfflinePlayer(u).getName();
                    if (n == null) n = u.toString();
                    player.sendMessage(ChatColor.GRAY + "- " + n);
                }
            }
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /ignore <player|list>");
            return true;
        }

        OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
        if (off.getName() == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        UUID targetId = off.getUniqueId();
        if (manager.ignore(id, targetId)) {
            player.sendMessage(ChatColor.YELLOW + "Ignoring " + off.getName() + ".");
            manager.apply(player);
        } else {
            player.sendMessage(ChatColor.RED + "Already ignoring this player.");
        }
        return true;
    }
}

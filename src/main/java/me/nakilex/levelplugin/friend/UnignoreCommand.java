package me.nakilex.levelplugin.friend;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Command to remove players from ignore list. */
public class UnignoreCommand implements CommandExecutor {
    private final IgnoreManager manager;

    public UnignoreCommand(IgnoreManager manager) {
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

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /unignore <player>");
            return true;
        }

        OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
        if (off.getName() == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        UUID targetId = off.getUniqueId();
        if (manager.unignore(id, targetId)) {
            player.sendMessage(ChatColor.YELLOW + "No longer ignoring " + off.getName() + ".");
            manager.apply(player);
        } else {
            player.sendMessage(ChatColor.RED + "You were not ignoring that player.");
        }
        return true;
    }
}

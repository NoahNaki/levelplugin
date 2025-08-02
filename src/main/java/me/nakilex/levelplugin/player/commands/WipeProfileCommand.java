package me.nakilex.levelplugin.player.commands;

import me.nakilex.levelplugin.player.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/**
 * Command: /wipeprofile <player>
 * Completely resets a player's stats, level and currency.
 */
public class WipeProfileCommand implements CommandExecutor {

    public WipeProfileCommand() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /wipeprofile <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        ProfileManager.getInstance().wipePlayer(target);

        target.sendMessage(ChatColor.RED + "Your profile has been wiped by an administrator.");
        sender.sendMessage(ChatColor.GREEN + "Wiped profile of " + target.getName() + ".");
        return true;
    }
}

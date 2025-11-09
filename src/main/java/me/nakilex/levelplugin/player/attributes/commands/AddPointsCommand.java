package me.nakilex.levelplugin.player.attributes.commands;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import me.nakilex.levelplugin.utils.CommandUtil;

public class AddPointsCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Usage: /addpoints <player> <amount>
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /addpoints <player> <amount>");
            return true;
        }

        String playerName = args[0];
        int amount;

        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[1]);
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cPlease specify a positive integer amount.");
            return true;
        }

        // Fetch player's UUID from name
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        if (targetUUID == null) {
            sender.sendMessage("§cCould not find player " + playerName);
            return true;
        }

        // Grant skill points using UUID
        StatsManager.getInstance().addSkillPoints(targetUUID, amount);
        sender.sendMessage("§aGave " + amount + " skill points to " + playerName);

        // Notify the player if they are online
        if (Bukkit.getPlayer(targetUUID) != null) {
            Bukkit.getPlayer(targetUUID).sendMessage("§aYou have received " + amount + " skill points!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.onlinePlayerNames(args[0]);
        }
        if (args.length == 2) {
            return CommandUtil.numberOptions(args[1], 1, 5, 10, 25, 50, 100);
        }
        return Collections.emptyList();
    }
}

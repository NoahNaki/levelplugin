package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddPointsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Usage: /addpoints <player> <amount>
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /addpoints <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cCould not find player " + args[0]);
            return true;
        }

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

        // Grant skill points
        StatsManager.getInstance().addSkillPoints(target, amount);
        sender.sendMessage("§aGave " + amount + " skill points to " + target.getName());
        target.sendMessage("§aYou have received " + amount + " skill points!");

        return true;
    }
}

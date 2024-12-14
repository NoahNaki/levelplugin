package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddXPCommand implements CommandExecutor {

    private final LevelManager levelManager;

    public AddXPCommand(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Usage: /addxp <player> <amount>
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /addxp <player> <amount>");
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

        // Grant XP via LevelManager
        levelManager.addXP(target, amount);
        sender.sendMessage("§aGave " + amount + " XP to " + target.getName());
        target.sendMessage("§aYou have received " + amount + " XP!");

        return true;
    }
}

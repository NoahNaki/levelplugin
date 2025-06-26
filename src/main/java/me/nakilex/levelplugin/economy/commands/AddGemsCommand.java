package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.GemsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddGemsCommand implements CommandExecutor {
    private final GemsManager gemsManager;

    public AddGemsCommand(GemsManager gemsManager) {
        this.gemsManager = gemsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /addgems <player|@everyone> <amount>");
            return true;
        }

        String targetArg = args[0];
        int amt;
        try {
            amt = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("Invalid amount: " + args[1]);
            return true;
        }

        if (targetArg.equalsIgnoreCase("@everyone")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyAmount(p, amt);
            }
            sender.sendMessage("Adjusted everyone's gems by " + amt + ".");
        } else {
            Player target = Bukkit.getPlayer(targetArg);
            if (target == null) {
                sender.sendMessage("Player not found: " + targetArg);
                return true;
            }
            applyAmount(target, amt);
            sender.sendMessage("Adjusted " + target.getName() + "'s gems by " + amt + ".");
        }

        return true;
    }

    private void applyAmount(Player player, int amt) {
        int current = gemsManager.getTotalUnits(player);
        int newTotal = current + amt;
        if (newTotal < 0) newTotal = 0;
        gemsManager.setTotalUnits(player, newTotal);
        player.sendMessage((amt >= 0 ? "You received " : "You lost ")
                + Math.abs(amt) + " gems.");
    }
}

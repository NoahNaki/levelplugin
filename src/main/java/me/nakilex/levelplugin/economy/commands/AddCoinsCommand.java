package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;


public class AddCoinsCommand implements CommandExecutor {

    private EconomyManager economy;

    public AddCoinsCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if(args.length < 2) {
            sender.sendMessage("Usage: /addcoins <player|@everyone> <amount>");
            return true;
        }

        String targetArg = args[0];
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            sender.sendMessage("Invalid amount: " + args[1]);
            return true;
        }

        if (targetArg.equalsIgnoreCase("@everyone")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyAmount(p.getUniqueId(), amount, p);
            }
            sender.sendMessage("Adjusted everyone's coins by " + amount + ".");
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetArg);
            if (target.getName() == null) {
                sender.sendMessage("Player not found: " + targetArg);
                return true;
            }
            applyAmount(target.getUniqueId(), amount, target.getPlayer());
            sender.sendMessage("Adjusted " + target.getName() + "'s coins by " + amount + ".");
        }

        return true;
    }

    private void applyAmount(UUID playerId, int amount, Player online) {
        int current = economy.getBalance(playerId);
        int newBal = current + amount;
        if (newBal < 0) newBal = 0;
        economy.setBalance(playerId, newBal);
        if (online != null) {
            if (amount >= 0) {
                String coinText = Math.abs(amount) + " <glyph:coins_icon> "
                        + org.bukkit.ChatColor.GOLD + "coins";
                online.sendMessage(org.bukkit.ChatColor.GREEN + "You received "
                        + coinText + org.bukkit.ChatColor.GREEN + ".");
            } else {
                online.sendMessage("You lost " + Math.abs(amount) + " coins.");
            }
        }
    }
}

package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import me.nakilex.levelplugin.utils.CommandUtil;


public class AddCoinsCommand implements TabExecutor {

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
                me.nakilex.levelplugin.utils.CurrencyMessageUtil.sendReceive(online,
                        me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, amount);
            } else {
                me.nakilex.levelplugin.utils.CurrencyMessageUtil.sendLoss(online,
                        me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, Math.abs(amount));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>(CommandUtil.onlinePlayerNames(args[0]));
            if ("@everyone".startsWith(args[0].toLowerCase())) {
                names.add("@everyone");
            }
            return names;
        }
        return Collections.emptyList();
    }
}

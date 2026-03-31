package me.nakilex.levelplugin.economy.commands;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private EconomyManager economy;

    public BalanceCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }
        Player player = (Player)sender;
        if (args.length >= 1 && args[0].equalsIgnoreCase("pay")) {
            if (args.length < 3) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Usage: /balance pay <player> <amount>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target.getUniqueId() == null || (!target.hasPlayedBefore() && target.getPlayer() == null)) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Player not found.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Amount must be a number.");
                return true;
            }
            if (amount <= 0) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Amount must be greater than zero.");
                return true;
            }
            if (economy.getBalance(player) < amount) {
                ChatMessageUtil.send(player, MessageType.ERROR, "You do not have enough coins.");
                return true;
            }
            economy.deductCoins(player, amount);
            economy.addCoins(target.getUniqueId(), amount, false);
            ChatMessageUtil.send(player, MessageType.SUCCESS,
                    "Sent " + ChatColor.YELLOW + amount + ChatColor.GREEN + " coins to "
                            + ChatColor.YELLOW + (target.getName() == null ? "player" : target.getName()) + ChatColor.GREEN + ".");
            if (target.getPlayer() != null) {
                ChatMessageUtil.send(target.getPlayer(), MessageType.REWARD,
                        "You received " + ChatColor.YELLOW + amount + ChatColor.GOLD + " coins"
                                + ChatColor.GOLD + " from " + ChatColor.YELLOW + player.getName() + ChatColor.GOLD + ".");
            }
            return true;
        }
        int balance = economy.getBalance(player);
        ChatMessageUtil.send(player, MessageType.INFO,
                "Your balance: " + ChatColor.YELLOW + balance + ChatColor.WHITE + " coins.");
        return true;
    }
}

package me.nakilex.levelplugin.booster;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoosterCommand implements TabExecutor {

    private final double multiplier;

    public BoosterCommand(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatMessageUtil.format(MessageType.INFO,
                    "Available boosters: "
                            + ChatColor.YELLOW + "coin" + ChatColor.GRAY + ", "
                            + ChatColor.YELLOW + "xp" + ChatColor.GRAY
                            + ". Item multiplier: " + ChatColor.YELLOW + "x" + String.format("%.2f", multiplier)));
            return true;
        }

        if (args.length < 4 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatMessageUtil.format(MessageType.ERROR,
                    "Usage: /booster <list|give <player|@everyone> <coin|xp> <amount>>"));
            return true;
        }

        String target = args[1];
        BoosterType type = parseType(args[2]);
        if (type == null) {
            sender.sendMessage(ChatMessageUtil.format(MessageType.ERROR, "Unknown booster type: " + args[2]));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatMessageUtil.format(MessageType.ERROR, "Amount must be a number."));
            return true;
        }

        ItemStack boosterItem = BoosterItemUtil.createBoosterItem(type, amount, multiplier);
        if (target.equalsIgnoreCase("@everyone")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getInventory().addItem(boosterItem.clone());
            }
            sender.sendMessage(ChatMessageUtil.format(MessageType.SUCCESS,
                    "Gave everyone " + amount + " " + type.displayName() + ChatColor.RESET + ChatColor.GREEN + "."));
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);
            if (offlinePlayer.getPlayer() == null) {
                sender.sendMessage(ChatMessageUtil.format(MessageType.ERROR, "Player not found: " + target));
                return true;
            }
            offlinePlayer.getPlayer().getInventory().addItem(boosterItem);
            sender.sendMessage(ChatMessageUtil.format(MessageType.SUCCESS,
                    "Gave " + offlinePlayer.getName() + " " + amount + " " + type.displayName() + ChatColor.RESET + ChatColor.GREEN + "."));
        }
        return true;
    }

    private BoosterType parseType(String input) {
        if (input.equalsIgnoreCase("coin") || input.equalsIgnoreCase("coins")) return BoosterType.COIN;
        if (input.equalsIgnoreCase("xp") || input.equalsIgnoreCase("combat")) return BoosterType.COMBAT_XP;
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> roots = new ArrayList<>();
            if ("give".startsWith(args[0].toLowerCase())) roots.add("give");
            if ("list".startsWith(args[0].toLowerCase())) roots.add("list");
            return roots;
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>(CommandUtil.onlinePlayerNames(args[1]));
            if ("@everyone".startsWith(args[1].toLowerCase())) {
                names.add("@everyone");
            }
            return names;
        }
        if (args.length == 3) {
            List<String> suggestions = new ArrayList<>();
            if ("coin".startsWith(args[2].toLowerCase())) suggestions.add("coin");
            if ("xp".startsWith(args[2].toLowerCase())) suggestions.add("xp");
            return suggestions;
        }
        return Collections.emptyList();
    }
}

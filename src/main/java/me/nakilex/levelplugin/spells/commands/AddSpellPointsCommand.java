package me.nakilex.levelplugin.spells.commands;

import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AddSpellPointsCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /addsp <player> <amount>");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid amount: " + args[1]);
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be positive.");
            return true;
        }
        UUID targetId = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
        SpellProgressionManager.getInstance().addSpellPoints(targetId, amount);
        sender.sendMessage("§aGave " + amount + " spell points to " + args[0]);
        Player online = Bukkit.getPlayer(targetId);
        if (online != null) {
            online.sendMessage("§aYou received " + amount + " spell points.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.onlinePlayerNames(args[0]);
        }
        if (args.length == 2) {
            return CommandUtil.numberOptions(args[1], 1, 5, 10, 25, 50);
        }
        return Collections.emptyList();
    }
}

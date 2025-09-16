package me.nakilex.levelplugin.player.classes.commands;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SealingCharmCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /sealingcharm <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
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
            sender.sendMessage("§cAmount must be positive.");
            return true;
        }

        int remaining = amount;
        int maxStack = ItemUtil.createSealingCharm(1).getMaxStackSize();
        while (remaining > 0) {
            int give = Math.min(remaining, maxStack);
            target.getInventory().addItem(ItemUtil.createSealingCharm(give));
            remaining -= give;
        }

        String charmName = ItemUtil.SEALING_CHARM_NAME + ChatColor.RESET;
        sender.sendMessage("§aGave " + amount + "x " + charmName + " to " + target.getName() + ".");
        target.sendMessage("§aYou received " + amount + "x " + charmName + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.onlinePlayerNames(args[0]);
        }
        if (args.length == 2) {
            return CommandUtil.filterStartingWith(List.of("1", "2", "3", "4", "5", "10", "16", "32", "64"), args[1]);
        }
        return Collections.emptyList();
    }
}

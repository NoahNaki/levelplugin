package me.nakilex.levelplugin.items.commands;

import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

import me.nakilex.levelplugin.utils.CommandUtil;

public class AddItemCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /additem <level> <player> <amount>
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /additem <level> <player> <amount>");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level: " + args[0]);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[2]);
            return true;
        }

        CustomItem instance = ItemManager.getInstance().generateItem("command", Math.max(1, level));

        // Give the ItemStack to the player
        target.getInventory().addItem(
            ItemUtil.createItemStackFromCustomItem(instance, amount, target)
        );

        sender.sendMessage("§aGave " + amount + "x " + instance.getName()
            + " to " + target.getName() + ".");
        target.sendMessage("§aYou received " + amount + "x " + instance.getName() + "!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.numberOptions(args[0], 1, 5, 10, 20, 40, 60, 80, 100);
        }
        if (args.length == 2) {
            return CommandUtil.onlinePlayerNames(args[1]);
        }
        if (args.length == 3) {
            return CommandUtil.numberOptions(args[2], 1, 5, 10, 16, 32, 64);
        }
        return Collections.emptyList();
    }
}

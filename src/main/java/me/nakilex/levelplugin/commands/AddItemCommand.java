package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.managers.ItemManager;
import me.nakilex.levelplugin.items.CustomItem;
import me.nakilex.levelplugin.items.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /additem <numeric_item_id> <player> <amount>
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /additem <item_id> <player> <amount>");
            return true;
        }

        int itemId;
        try {
            itemId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid numeric ID: " + args[0]);
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

        CustomItem cItem = ItemManager.getInstance().getItemById(itemId);
        if (cItem == null) {
            sender.sendMessage("§cNo custom item found with ID: " + itemId);
            return true;
        }

        target.getInventory().addItem(ItemUtil.createItemStackFromCustomItem(cItem, amount));
        sender.sendMessage("§aGave " + amount + "x " + cItem.getName() + " [ID:" + itemId + "] to " + target.getName());
        target.sendMessage("§aYou received " + amount + "x " + cItem.getName() + "! (ID:" + itemId + ")");
        return true;
    }
}

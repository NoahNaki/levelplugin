package me.nakilex.levelplugin.items.commands;

import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
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

        // Fetch the template (which now holds StatRanges)
        CustomItem template = ItemManager.getInstance().getTemplateById(itemId);
        if (template == null) {
            sender.sendMessage("§cNo custom item found with ID: " + itemId);
            return true;
        }

        // Create a brand-new instance: UUID generated & stats rolled
        CustomItem instance = new CustomItem(
            template.getId(),
            template.getBaseName(),
            template.getRarity(),
            template.getLevelRequirement(),
            template.getClassRequirement(),
            template.getMaterial(),
            template.getHpRange(),
            template.getDefRange(),
            template.getStrRange(),
            template.getAgiRange(),
            template.getIntelRange(),
            template.getDexRange()
        );

        // Register it
        ItemManager.getInstance().addInstance(instance);

        // Give the ItemStack to the player
        target.getInventory().addItem(
            ItemUtil.createItemStackFromCustomItem(instance, amount, target)
        );

        sender.sendMessage("§aGave " + amount + "x " + instance.getName()
            + " [ID:" + itemId + "] to " + target.getName());
        target.sendMessage("§aYou received " + amount + "x " + instance.getName()
            + "! (ID:" + itemId + ")");

        return true;
    }
}

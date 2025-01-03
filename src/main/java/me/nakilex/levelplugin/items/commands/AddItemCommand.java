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

        CustomItem template = ItemManager.getInstance().getTemplateById(itemId); // Fetch template by ID
        if (template == null) {
            sender.sendMessage("§cNo custom item found with ID: " + itemId);
            return true;
        }

// Create a unique instance with a UUID
        CustomItem instance = new CustomItem(
            java.util.UUID.randomUUID(), // Generate new UUID
            template.getId(),
            template.getBaseName(),
            template.getRarity(),
            template.getLevelRequirement(),
            template.getClassRequirement(),
            template.getMaterial(),
            template.getHp(),
            template.getDef(),
            template.getStr(),
            template.getAgi(),
            template.getIntel(),
            template.getDex(),
            0 // Start at upgrade level 0
        );

// Add the instance to UUID map
        ItemManager.getInstance().addInstance(instance);

// Create ItemStack and give it to player
        target.getInventory().addItem(ItemUtil.createItemStackFromCustomItem(instance, amount));
        sender.sendMessage("§aGave " + amount + "x " + instance.getName() + " [ID:" + itemId + "] to " + target.getName());
        target.sendMessage("§aYou received " + amount + "x " + instance.getName() + "! (ID:" + itemId + ")");

        return true;
    }
}

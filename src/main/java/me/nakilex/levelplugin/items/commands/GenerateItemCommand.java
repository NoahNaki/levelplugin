package me.nakilex.levelplugin.items.commands;

import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GenerateItemCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /genitem <player> <mobType> <level>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        String mobType = args[1];
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level: " + args[2]);
            return true;
        }

        CustomItem item = ItemManager.getInstance().generateItem(mobType, level);
        target.getInventory().addItem(ItemUtil.createItemStackFromCustomItem(item, 1, target));
        sender.sendMessage("§aGenerated " + item.getName() + " for " + target.getName());
        return true;
    }
}

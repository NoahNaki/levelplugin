package me.nakilex.levelplugin.items.commands;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import net.kyori.adventure.key.Key;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * /addlore [index] [text]
 *
 * Simple utility mirroring ItemsAdder's lore editing command. Inserts a line
 * of lore into the held item at the specified index. Use the {@code style}
 * subcommand to change the item's tooltip border.
 */
public class AddLoreCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item first.");
            return true;
        }

        // Subcommand: /addlore style <name>
        if (args.length >= 2 && args[0].equalsIgnoreCase("style")) {
            String raw = args[1];
            String style = raw.contains(":") ? raw : "minecraft:" + raw.toLowerCase();
            ItemUtil.setKeyedComponent(stack, DataComponentTypes.TOOLTIP_STYLE, Key.key(style));
            player.getInventory().setItemInMainHand(stack);
            player.sendMessage(ChatColor.GREEN + "Tooltip style set to " + style + ".");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /addlore <index> <text>");
            return true;
        }

        int index;
        try {
            index = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid line index: " + args[0]);
            return true;
        }

        String line = ChatColor.translateAlternateColorCodes('&',
                String.join(" ", Arrays.copyOfRange(args, 1, args.length)));

        ItemUtil.insertLoreLine(stack, index, line);
        player.getInventory().setItemInMainHand(stack);
        player.sendMessage(ChatColor.GREEN + "Lore line added.");
        return true;
    }
}

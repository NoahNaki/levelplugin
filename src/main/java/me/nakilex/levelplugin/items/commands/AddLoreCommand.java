package me.nakilex.levelplugin.items.commands;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.nakilex.levelplugin.items.data.ItemRarity;
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
 * If text is omitted, a rarity glyph line is inserted at the top of the held
 * item's lore. Otherwise behaves like the ItemsAdder command where the first
 * argument is the line index and the rest form the lore text.
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

        int index = 0;
        String line;

        if (args.length >= 2) {
            try {
                index = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid line index: " + args[0]);
                return true;
            }
            line = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        } else {
            ItemRarity rarity = ItemUtil.getItemRarity(stack);
            if (rarity == null) {
                player.sendMessage(ChatColor.RED + "Could not determine item rarity.");
                return true;
            }
            line = rarity.getSymbol() + "<glyph:item>";
            String style = "minecraft:" + rarity.name().toLowerCase();
            ItemUtil.setKeyedComponent(stack, DataComponentTypes.TOOLTIP_STYLE, Key.key(style));
        }

        ItemUtil.insertLoreLine(stack, index, line);
        player.getInventory().setItemInMainHand(stack);
        player.sendMessage(ChatColor.GREEN + "Lore line added.");
        return true;
    }
}

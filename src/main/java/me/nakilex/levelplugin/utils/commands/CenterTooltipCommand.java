package me.nakilex.levelplugin.utils.commands;

import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * /centertooltip - give the player an item with a centered tooltip.
 */
public class CenterTooltipCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aCentered Tooltip");
            meta.setLore(Arrays.asList("§7This lore", "§7is centered"));
            item.setItemMeta(meta);
        }

        TextUtil.centerItemTooltip(item);
        player.getInventory().addItem(item);
        player.sendMessage("§aEnjoy your centered item!");
        return true;
    }
}

package me.nakilex.levelplugin.utils.commands;

import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * /centergui - open a simple GUI with a centered title.
 */
public class CenterGuiCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;

        String title = TextUtil.centerInventoryTitle("Centered GUI");
        Inventory inv = Bukkit.createInventory(player, 9, title);
        player.openInventory(inv);
        return true;
    }
}

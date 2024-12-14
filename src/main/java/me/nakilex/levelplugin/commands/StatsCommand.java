package me.nakilex.levelplugin.commands;

import me.nakilex.levelplugin.ui.StatsInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /stats
 * Opens the stats GUI for the player.
 */
public class StatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        player.openInventory(StatsInventory.getStatsMenu(player));

        return true;
    }
}

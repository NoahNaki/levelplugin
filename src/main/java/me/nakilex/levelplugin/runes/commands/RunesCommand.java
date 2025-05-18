package me.nakilex.levelplugin.runes.commands;

import me.nakilex.levelplugin.runes.gui.RuneInventoryGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the "/runes" command to open the Rune Inventory GUI.
 */
public class RunesCommand implements CommandExecutor {
    private final RuneInventoryGUI gui;

    public RunesCommand(RuneInventoryGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can open the Rune Inventory.");
            return true;
        }
        gui.openInventory((Player) sender);
        return true;
    }
}

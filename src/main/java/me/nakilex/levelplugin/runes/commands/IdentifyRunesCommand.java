package me.nakilex.levelplugin.runes.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.gui.IdentifyRunesGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the Identify Runes GUI. NPCs or console can call this.
 */
public class IdentifyRunesCommand implements CommandExecutor {
    private final IdentifyRunesGUI gui;

    public IdentifyRunesCommand(IdentifyRunesGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can identify runes.");
            return true;
        }
        gui.openInventory((Player) sender);
        return true;
    }
}

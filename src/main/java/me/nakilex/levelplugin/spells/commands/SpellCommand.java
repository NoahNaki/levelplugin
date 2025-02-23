package me.nakilex.levelplugin.spells.commands;

import me.nakilex.levelplugin.spells.gui.SpellGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpellCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Make sure only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players!");
            return true;
        }

        Player player = (Player) sender;

        // Open the GUI
        SpellGUI.openSpellGUI(player);

        return true; // Command handled successfully
    }
}

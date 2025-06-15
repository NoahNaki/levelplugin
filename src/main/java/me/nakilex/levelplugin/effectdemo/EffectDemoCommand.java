package me.nakilex.levelplugin.effectdemo;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the FX demo GUI.
 */
public class EffectDemoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }
        EffectDemoGUI.open((Player) sender);
        return true;
    }
}

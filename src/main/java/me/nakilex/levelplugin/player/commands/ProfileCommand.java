package me.nakilex.levelplugin.player.commands;

import me.nakilex.levelplugin.player.profile.ProfileSelectionGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command to reopen the profile selection menu. */
public class ProfileCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        ProfileSelectionGUI.startSelection(player);
        return true;
    }
}

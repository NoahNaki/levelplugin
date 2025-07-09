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
        // If the player already has a profile selected, simply open the
        // menu without restricting movement.  The selection enforcement
        // logic is only needed when no profile is active (handled on join).
        if (me.nakilex.levelplugin.player.profile.ProfileManager.getInstance()
                .getActiveSlot(player.getUniqueId()) == null) {
            ProfileSelectionGUI.startSelection(player);
        } else {
            ProfileSelectionGUI.open(player);
        }
        return true;
    }
}

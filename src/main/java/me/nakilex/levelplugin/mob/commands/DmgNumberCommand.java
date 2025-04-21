package me.nakilex.levelplugin.mob.commands;

import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor for /dmgnumber.
 * Toggles the floating damage indicators on or off for the player.
 */
public class DmgNumberCommand implements CommandExecutor {

    private final DmgNumberToggleManager toggleManager;

    public DmgNumberCommand(DmgNumberToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can toggle damage numbers.");
            return true;
        }

        Player player = (Player) sender;
        boolean nowEnabled = toggleManager.toggle(player);

        String status = nowEnabled
            ? ChatColor.GREEN + "Damage numbers: ON"
            : ChatColor.RED + "Damage numbers: OFF";

        player.sendMessage(status);
        return true;
    }
}

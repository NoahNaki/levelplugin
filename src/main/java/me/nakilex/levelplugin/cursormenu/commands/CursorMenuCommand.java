package me.nakilex.levelplugin.cursormenu.commands;

import me.nakilex.levelplugin.cursormenu.CursorMenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Simple command executor exposing minimal functionality of the cursor menu
 * system. This is primarily used for testing and demonstration.
 */
public class CursorMenuCommand implements CommandExecutor {

    private final CursorMenuManager menuManager;

    public CursorMenuCommand(CursorMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " run <menu>|stop");
            return true;
        }

        if (args[0].equalsIgnoreCase("run") && args.length > 1) {
            menuManager.setupCursor(player, args[1]);
            sender.sendMessage("Opened cursor menu " + args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            menuManager.stopCursor(player, true);
            sender.sendMessage("Closed cursor menu");
            return true;
        }

        sender.sendMessage("Unknown sub command");
        return true;
    }
}

package me.nakilex.levelplugin.cursormenu.commands;

import me.nakilex.levelplugin.cursormenu.CursorMenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Basic command wrapper around the {@link CursorMenuManager} for testing.
 * Usage: /cursormenu run <section> or /cursormenu stop
 */
public class CursorMenuCommand implements CommandExecutor {
    private final CursorMenuManager manager;

    public CursorMenuCommand(CursorMenuManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " run <section>|stop");
            return true;
        }
        if (args[0].equalsIgnoreCase("run") && args.length > 1) {
            manager.setupCursor(player, args[1]);
            return true;
        }
        if (args[0].equalsIgnoreCase("stop")) {
            manager.stopCursor(player);
            return true;
        }
        sender.sendMessage("Usage: /" + label + " run <section>|stop");
        return true;
    }
}

package me.nakilex.levelplugin.screenmenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to control screen menus. Usage:
 * /cursormenu run <menu>
 * /cursormenu stop
 * /cursormenu reload
 */
public class ScreenMenuCommand implements CommandExecutor {

    private final ScreenMenuManager manager;

    public ScreenMenuCommand(ScreenMenuManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /" + label + " <run|stop|reload> [menu]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (args.length < 2) {
                    player.sendMessage("Specify a menu id.");
                    return true;
                }
                boolean shown = manager.showMenu(player, args[1]);
                if (!shown) player.sendMessage("Unknown menu: " + args[1]);
            }
            case "stop" -> manager.hideMenu(player);
            case "reload" -> {
                manager.reload();
                player.sendMessage("Screen menus reloaded.");
            }
            default -> player.sendMessage("Usage: /" + label + " <run|stop|reload> [menu]");
        }
        return true;
    }
}


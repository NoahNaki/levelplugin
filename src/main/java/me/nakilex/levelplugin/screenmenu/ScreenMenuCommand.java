package me.nakilex.levelplugin.screenmenu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Basic command to control screen menus.
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
            sender.sendMessage("Usage: /" + label + " <run|stop|reload> [menu]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (args.length < 2) {
                    sender.sendMessage("Specify a menu name.");
                } else {
                    manager.showMenu(player, args[1]);
                }
            }
            case "stop" -> manager.closeMenu(player);
            case "reload" -> {
                manager.reload();
                sender.sendMessage("Screen menus reloaded.");
            }
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }
}

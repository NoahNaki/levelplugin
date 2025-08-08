package me.nakilex.levelplugin.screen.commands;

import me.nakilex.levelplugin.screen.CursorMenuManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Simple command for interacting with cursor menus.
 * Currently supports stopping the active menu and listing available menus.
 */
public class CursorMenuCommand implements CommandExecutor {

    private final CursorMenuManager menuManager;

    public CursorMenuCommand(CursorMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/cursormenu <open|stop|list> [menu]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can open cursor menus.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "/cursormenu open <menu>");
                    return true;
                }
                String key = args[1];
                if (menuManager.getSectionManager().get(key) == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown cursor menu: " + key);
                    return true;
                }
                menuManager.openMenu(player, key);
                sender.sendMessage(ChatColor.GREEN + "Opened cursor menu \"" + key + "\".");
                return true;
            case "stop":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can stop cursor menus.");
                    return true;
                }
                boolean closed = menuManager.closeMenu(player);
                if (closed) {
                    sender.sendMessage(ChatColor.GREEN + "Cursor menu closed.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You have no cursor menu open.");
                }
                return true;
            case "list":
                sender.sendMessage(ChatColor.GOLD + "Cursor menus: " + String.join(", ", menuManager.getSectionManager().keySet()));
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "/cursormenu <open|stop|list> [menu]");
                return true;
        }
    }
}


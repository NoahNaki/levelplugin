package me.nakilex.levelplugin.cursormenu.command;

import me.nakilex.levelplugin.cursormenu.CursorMenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for /cursormenu.
 */
public class CursorMenuCommand implements CommandExecutor, TabCompleter {
    private final CursorMenuManager manager;

    public CursorMenuCommand(CursorMenuManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) return false;
        switch (args[0].toLowerCase()) {
            case "run":
                if (args.length >= 2) {
                    manager.setupCursor(player, args[1]);
                }
                return true;
            case "stop":
                manager.stopCursor(player, true);
                return true;
            case "reload":
                if (player.hasPermission("cursormenu.reload")) {
                    manager.reloadPluginConfig();
                    sender.sendMessage("CursorMenu reloaded.");
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("run");
            options.add("stop");
            options.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            options.addAll(manager.getSectionManager().keySet());
        }
        return options;
    }
}

package me.nakilex.levelplugin.cursormenu.command;

import me.nakilex.levelplugin.cursormenu.CursorMenuPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Basic command handler for the cursor menu system.
 */
public class CursorMenuCommand implements CommandExecutor, TabCompleter {
    private final CursorMenuPlugin plugin;

    public CursorMenuCommand(CursorMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " <run|stop|reload> [menu]");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "run" -> {
                if (args.length < 2) {
                    sender.sendMessage("Specify a menu to run.");
                    return true;
                }
                plugin.setupCursor(player, args[1]);
            }
            case "stop" -> plugin.stopCursor(player);
            case "reload" -> {
                plugin.reloadConfigs();
                sender.sendMessage("Cursor menu reloaded.");
            }
            default -> sender.sendMessage("Unknown subcommand.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("run");
            completions.add("stop");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            completions.addAll(plugin.getSectionManager().keySet());
        }
        return completions;
    }
}

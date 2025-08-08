package me.nakilex.levelplugin.screen.command;

import me.nakilex.levelplugin.screen.CursorMenuSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple command interface for testing the cursor menus.
 */
public class CursorMenuCommand implements CommandExecutor, TabCompleter {
    private final CursorMenuSystem system;

    public CursorMenuCommand(CursorMenuSystem system) {
        this.system = system;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) return false;
        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (args.length > 1) {
                    system.setupCursor(player, args[1]);
                }
            }
            case "stop" -> system.stopCursor(player);
            default -> {
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("run", "stop");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            List<String> keys = new ArrayList<>();
            system.getSectionManager().keySet().forEach(k -> keys.add(k));
            return keys;
        }
        return List.of();
    }
}

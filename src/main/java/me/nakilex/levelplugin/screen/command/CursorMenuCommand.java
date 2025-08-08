package me.nakilex.levelplugin.screen.command;

import me.nakilex.levelplugin.screen.CursorMenuSystem;
import me.nakilex.levelplugin.screen.menu.Section;
import org.bukkit.ChatColor;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /cursormenu <run|stop>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /cursormenu run <section>");
                    return true;
                }

                Section section = system.getSectionManager().get(args[1]);
                if (section == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown section: " + args[1]);
                    return true;
                }
                if (section.getPermission() != null && !section.getPermission().isEmpty() && !player.hasPermission(section.getPermission())) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission for this menu.");
                    return true;
                }

                system.setupCursor(player, section.getKey());
                sender.sendMessage(ChatColor.GREEN + "Opened cursor menu: " + section.getKey());
            }
            case "stop" -> {
                if (!system.isInMenu(player)) {
                    sender.sendMessage(ChatColor.RED + "You are not using a cursor menu.");
                    return true;
                }
                system.stopCursor(player);
                sender.sendMessage(ChatColor.YELLOW + "Closed cursor menu.");
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /cursormenu <run|stop>");
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

package me.nakilex.levelplugin.commands;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * Administrative command for LevelPlugin maintenance tasks such as
 * reloading configuration files at runtime.
 */
public class LevelPluginCommand implements TabExecutor {

    private static final String SUB_RELOAD = "reload";

    private final Main plugin;

    public LevelPluginCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !SUB_RELOAD.equalsIgnoreCase(args[0])) {
            ChatMessageUtil.send(sender, MessageType.INFO, "Usage: /" + label + " reload");
            return true;
        }
        if (!sender.hasPermission("levelplugin.admin")) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "You do not have permission to do that.");
            return true;
        }

        long start = System.currentTimeMillis();
        plugin.reloadConfigValues();
        long elapsed = System.currentTimeMillis() - start;
        ChatMessageUtil.send(sender, MessageType.SUCCESS,
                "Reloaded LevelPlugin configuration in " + elapsed + "ms.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return SUB_RELOAD.startsWith(input)
                    ? List.of(SUB_RELOAD)
                    : Collections.emptyList();
        }
        return Collections.emptyList();
    }
}

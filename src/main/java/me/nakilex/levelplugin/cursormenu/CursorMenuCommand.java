package me.nakilex.levelplugin.cursormenu;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CursorMenuCommand implements CommandExecutor, TabCompleter {
    private final CursorMenuManager manager;

    public CursorMenuCommand(CursorMenuManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "run" -> {
                if (args.length < 2) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            ChatColor.RED + "Usage: /cursormenu run <menu>");
                    return true;
                }
                manager.runMenu(player, args[1]);
                return true;
            }
            case "stop" -> {
                boolean stopped = manager.stopMenu(player, true);
                ChatMessageUtil.send(player, stopped ? ChatMessageUtil.MessageType.SUCCESS : ChatMessageUtil.MessageType.WARNING,
                        stopped ? ChatColor.GREEN + "Closed cursor menu." : ChatColor.RED + "No active cursor menu.");
                return true;
            }
            case "items" -> {
                if (args.length < 2) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            ChatColor.RED + "Usage: /cursormenu items <id>");
                    return true;
                }
                manager.showItemPreview(player, args[1]);
                return true;
            }
            case "itemsstop" -> {
                boolean removed = manager.hideItemPreview(player);
                ChatMessageUtil.send(player, removed ? ChatMessageUtil.MessageType.SUCCESS : ChatMessageUtil.MessageType.WARNING,
                        removed ? ChatColor.GREEN + "Stopped cursor item preview." : ChatColor.RED + "No active cursor item preview.");
                return true;
            }
            case "reload" -> {
                manager.reload();
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        ChatColor.GREEN + "Reloaded cursor menu configs.");
                return true;
            }
            default -> {
                sendUsage(player);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.simpleSuggestions(args[0], "run", "stop", "items", "itemsstop", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            return CommandUtil.filterStartingWith(manager.getMenuKeys(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("items")) {
            return CommandUtil.filterStartingWith(manager.getItemPresetKeys(), args[1]);
        }
        return Collections.emptyList();
    }

    private void sendUsage(Player player) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "CursorMenu Commands: " + ChatColor.WHITE
                        + "/cursormenu run <menu>, /cursormenu stop, /cursormenu items <id>, /cursormenu itemsstop, /cursormenu reload");
    }
}

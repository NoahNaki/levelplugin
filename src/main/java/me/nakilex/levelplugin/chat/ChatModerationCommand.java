package me.nakilex.levelplugin.chat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;

/**
 * Command handler for muting/unmuting and clearing chat.
 */
public class ChatModerationCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "mute" -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
                    ChatManager.muteAll();
                    sender.sendMessage(ChatColor.YELLOW + "Chat has been muted.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /mute all");
                }
                return true;
            }
            case "unmute" -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
                    ChatManager.unmuteAll();
                    sender.sendMessage(ChatColor.YELLOW + "Chat has been unmuted.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /unmute all");
                }
                return true;
            }
            case "clearchat" -> {
                ChatManager.clearChat();
                sender.sendMessage(ChatColor.YELLOW + "Chat cleared.");
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ((command.getName().equalsIgnoreCase("mute") || command.getName().equalsIgnoreCase("unmute")) && args.length == 1) {
            return List.of("all");
        }
        return Collections.emptyList();
    }
}

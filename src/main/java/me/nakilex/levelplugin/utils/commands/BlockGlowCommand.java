package me.nakilex.levelplugin.utils.commands;

import me.nakilex.levelplugin.utils.BlockGlowUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Standalone command for block glow testing/usage.
 */
public class BlockGlowCommand implements CommandExecutor, TabCompleter {
    private final BlockGlowUtil blockGlowUtil;

    public BlockGlowCommand(BlockGlowUtil blockGlowUtil) {
        this.blockGlowUtil = blockGlowUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (blockGlowUtil == null || !blockGlowUtil.isSupported()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    ChatColor.RED + "Block glow bridge is unavailable on this server.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "look" -> {
                Block target = player.getTargetBlockExact(40);
                if (target == null || target.getType().isAir()) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            ChatColor.RED + "Look at a solid block first.");
                    return true;
                }

                ChatColor color = parseColor(args.length >= 2 ? args[1] : "AQUA");
                int duration = parseInt(args.length >= 3 ? args[2] : "20", 20);
                blockGlowUtil.setGlowing(target, player, color, duration);

                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        ChatColor.GREEN + "Highlighted block " + ChatColor.WHITE + target.getType()
                                + ChatColor.GREEN + " for " + ChatColor.WHITE + duration + ChatColor.GREEN + " ticks.");
                return true;
            }
            case "clear" -> {
                blockGlowUtil.clearGlowing(player);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        ChatColor.GREEN + "Cleared your glowing blocks.");
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
            return CommandUtil.simpleSuggestions(args[0], "look", "clear");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("look")) {
            return CommandUtil.simpleSuggestions(args[1], "AQUA", "GREEN", "YELLOW", "RED", "LIGHT_PURPLE", "WHITE");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("look")) {
            return CommandUtil.numberOptions(args[2], 10, 20, 40, 60, 100);
        }
        return Collections.emptyList();
    }

    private void sendUsage(Player player) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/blockglow look [color] [durationTicks], /blockglow clear");
    }

    private static ChatColor parseColor(String name) {
        if (name == null || name.isBlank()) {
            return ChatColor.AQUA;
        }
        try {
            return ChatColor.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChatColor.AQUA;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}

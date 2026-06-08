package me.nakilex.levelplugin.luxbridge.command;

import me.nakilex.levelplugin.luxbridge.LuxBridgeManager;
import me.nakilex.levelplugin.luxbridge.model.LuxDialogue;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LuxBridgeCommand implements CommandExecutor, TabCompleter {
    private final LuxBridgeManager manager;

    public LuxBridgeCommand(LuxBridgeManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                manager.reload();
                sender.sendMessage(ChatColor.GREEN + "LuxBridge reloaded. Loaded " + manager.dialogues().size() + " dialogue(s).");
            }
            case "start" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can start a LuxBridge dialogue.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /luxbridge start <dialogueId>");
                    return true;
                }
                try {
                    manager.start(player, args[1]);
                    sender.sendMessage(ChatColor.GREEN + "Started LuxBridge dialogue " + args[1] + ".");
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + exception.getMessage());
                }
            }
            case "stop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can stop their LuxBridge dialogue.");
                    return true;
                }
                manager.stop(player);
                sender.sendMessage(ChatColor.YELLOW + "Stopped LuxBridge dialogue.");
            }
            case "assets" -> {
                manager.reload();
                for (String line : manager.assetDiagnostics()) {
                    sender.sendMessage(line);
                }
                manager.logAssetDiagnostics();
            }
            case "inspect" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /luxbridge inspect <dialogueId>");
                    return true;
                }
                LuxDialogue dialogue = manager.getDialogue(args[1]);
                if (dialogue == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown dialogue: " + args[1]);
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "LuxBridge " + dialogue.id());
                sender.sendMessage(ChatColor.GRAY + "Pages: " + ChatColor.WHITE + dialogue.pages().size());
                sender.sendMessage(ChatColor.GRAY + "Images: " + ChatColor.WHITE + dialogue.dialogueBackgroundImage() + ", " + dialogue.answerBackgroundImage() + ", " + dialogue.characterBackgroundImage());
                sender.sendMessage(ChatColor.GRAY + "Namespace: " + ChatColor.WHITE + "levelplugin_dialogue");
            }
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatUtil.applyEmojis(ChatColor.GOLD + "LuxBridge" + ChatColor.GRAY + ": /luxbridge <reload|assets|start|stop|inspect>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("reload", "assets", "start", "stop", "inspect"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("inspect"))) {
            return filter(manager.dialogues().stream().map(LuxDialogue::id).toList(), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) matches.add(value);
        return matches;
    }
}

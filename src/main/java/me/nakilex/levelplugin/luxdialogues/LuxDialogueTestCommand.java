package me.nakilex.levelplugin.luxdialogues;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LuxDialogueTestCommand implements TabExecutor {
    private final JavaPlugin plugin;

    public LuxDialogueTestCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can test LuxDialogues dialogues.");
            return true;
        }

        String subCommand = args.length == 0 ? "send" : args[0].toLowerCase(Locale.ROOT);

        if (!LuxDialoguesBridge.isPluginEnabled()) {
            player.sendMessage(ChatColor.RED + "LuxDialogues is not enabled. Install/start LuxDialogues first, then retry /" + label + ".");
            return true;
        }

        try {
            switch (subCommand) {
                case "send", "start", "test" -> {
                    LuxDialoguesBridge.sendTestDialogue(player);
                    player.sendMessage(ChatColor.GREEN + "Sent LuxDialogues API test dialogue.");
                }
                case "clear", "stop", "end" -> {
                    LuxDialoguesBridge.clearDialogue(player);
                    player.sendMessage(ChatColor.YELLOW + "Cleared your active LuxDialogues dialogue.");
                }
                case "status" -> {
                    boolean inDialogue = LuxDialoguesBridge.isInDialogue(player);
                    player.sendMessage(ChatColor.AQUA + "LuxDialogues active dialogue: " + inDialogue);
                }
                default -> player.sendMessage(ChatColor.RED + "Usage: /" + label + " [send|status|clear]");
            }
        } catch (Throwable throwable) {
            plugin.getLogger().severe("LuxDialogues API test failed: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage(ChatColor.RED + "LuxDialogues API test failed. Check console for the stacktrace.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = Arrays.asList("send", "status", "clear");
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}

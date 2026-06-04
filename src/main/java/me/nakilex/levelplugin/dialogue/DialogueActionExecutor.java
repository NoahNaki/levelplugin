package me.nakilex.levelplugin.dialogue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DialogueActionExecutor {
    private final JavaPlugin plugin;
    private final DialoguePlaceholderFormatter formatter;

    public DialogueActionExecutor(JavaPlugin plugin, DialoguePlaceholderFormatter formatter) {
        this.plugin = plugin;
        this.formatter = formatter;
    }

    public void execute(Player player, DialogueSession session, String rawAction) {
        if (player == null || rawAction == null || rawAction.isBlank()) return;
        String action = formatter.format(player, rawAction).trim();
        if (action.startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.substring("console:".length()).trim());
        } else if (action.startsWith("command:")) {
            player.performCommand(action.substring("command:".length()).trim());
        } else if (action.startsWith("message:")) {
            player.sendMessage(action.substring("message:".length()).trim());
        } else if (!action.startsWith("callback:")) {
            plugin.getLogger().fine("Unhandled dialogue action: " + action);
        }
    }
}

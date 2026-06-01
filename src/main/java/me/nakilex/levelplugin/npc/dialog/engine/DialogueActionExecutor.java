package me.nakilex.levelplugin.npc.dialog.engine;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DialogueActionExecutor {
    private final Main plugin;
    private final Map<String, Consumer<Player>> callbacks = new ConcurrentHashMap<>();

    public DialogueActionExecutor(Main plugin) {
        this.plugin = plugin;
    }

    public void registerCallback(String id, Consumer<Player> callback) {
        if (id != null && callback != null) callbacks.put(id.toLowerCase(), callback);
    }

    public void run(Player player, List<String> actions) {
        for (String raw : actions) run(player, raw);
    }

    private void run(Player player, String raw) {
        if (raw == null || raw.isBlank()) return;
        String[] parts = raw.split(":", 2);
        String type = parts[0].toLowerCase();
        String value = parts.length == 2 ? parts[1] : "";
        switch (type) {
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replace("<player>", player.getName()));
            case "player" -> player.performCommand(value);
            case "message" -> player.sendMessage(ChatUtil.applyEmojis(value.replace("<player>", player.getName())));
            case "sound" -> playSound(player, value);
            case "quest-start" -> plugin.getQuestManager().startQuest(player, value);
            case "quest-complete" -> plugin.getQuestManager().completeQuest(player.getUniqueId(), value);
            case "flag-set" -> changeFlag(player, value, true);
            case "flag-remove" -> changeFlag(player, value, false);
            case "callback" -> {
                Consumer<Player> callback = callbacks.get(value.toLowerCase());
                if (callback != null) callback.accept(player);
            }
            default -> plugin.getLogger().warning("Unknown dialogue action: " + raw);
        }
    }

    private void playSound(Player player, String value) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(value.toUpperCase()), 1f, 1f);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unknown dialogue sound: " + value);
        }
    }

    private void changeFlag(Player player, String value, boolean set) {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return;
        if (set) plugin.getQuestManager().setFlag(player.getUniqueId(), parts[0], parts[1]);
        else plugin.getQuestManager().removeFlag(player.getUniqueId(), parts[0], parts[1]);
    }
}

package me.nakilex.levelplugin.npc.dialog.engine;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public final class DialogueConditionEvaluator {
    private final Main plugin;

    public DialogueConditionEvaluator(Main plugin) {
        this.plugin = plugin;
    }

    public boolean matches(DialogueSession session, List<String> conditions) {
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null) return false;
        for (String condition : conditions) {
            if (!matches(player, condition)) return false;
        }
        return true;
    }

    private boolean matches(Player player, String raw) {
        if (raw == null || raw.isBlank()) return true;
        String condition = raw.trim();
        QuestManager quests = plugin.getQuestManager();
        if (condition.startsWith("quest-active:")) return quests.getProgress(player.getUniqueId(), value(condition)) != null;
        if (condition.startsWith("quest-completed:")) return quests.hasCompleted(player.getUniqueId(), value(condition));
        if (condition.startsWith("permission:")) return player.hasPermission(value(condition));
        if (condition.startsWith("flag:")) return flagMatches(quests, player, value(condition), true);
        if (condition.startsWith("!flag:")) return flagMatches(quests, player, condition.substring("!flag:".length()), false);
        if (condition.startsWith("placeholder:")) return placeholderMatches(player, value(condition));
        return false;
    }

    private boolean flagMatches(QuestManager quests, Player player, String value, boolean expected) {
        String[] parts = value.split(":", 2);
        return parts.length == 2 && quests.hasFlag(player.getUniqueId(), parts[0], parts[1]) == expected;
    }

    private boolean placeholderMatches(Player player, String expression) {
        String[] parts = expression.split("=", 2);
        String resolved = DialogueTextFormatter.formatPlaceholders(player, parts[0]);
        return parts.length == 2 ? resolved.equalsIgnoreCase(parts[1]) : !resolved.isBlank() && !resolved.equalsIgnoreCase("false");
    }

    private String value(String condition) {
        return condition.substring(condition.indexOf(':') + 1);
    }
}

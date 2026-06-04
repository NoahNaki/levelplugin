package me.nakilex.levelplugin.dialogue;

import org.bukkit.entity.Player;

public class DialogueConditionEvaluator {
    public boolean canUse(Player player, String condition) {
        if (condition == null || condition.isBlank()) return true;
        String value = condition.trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return true;
    }
}

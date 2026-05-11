package me.nakilex.levelplugin.spells.summon;

import me.nakilex.levelplugin.spells.deck.SpellCardDefinition;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public final class SpellPullSummaryUtil {
    private SpellPullSummaryUtil() {
    }

    public static void sendSummary(Player player, String label, Map<SpellCardDefinition, Integer> pulls) {
        if (player == null || pulls == null || pulls.isEmpty()) {
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, ChatColor.GRAY + label + ":");
        for (var entry : pulls.entrySet()) {
            SpellCardDefinition card = entry.getKey();
            int count = entry.getValue();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    ChatColor.DARK_GRAY + "- " + card.rarity().color() + card.displayName()
                            + ChatColor.GRAY + " x" + count);
        }
    }
}

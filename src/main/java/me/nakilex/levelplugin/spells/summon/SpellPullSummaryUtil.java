package me.nakilex.levelplugin.spells.summon;

import me.nakilex.levelplugin.spells.deck.SpellCardDefinition;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager.SpellPullResult;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public final class SpellPullSummaryUtil {
    private SpellPullSummaryUtil() {
    }

    public static void sendPullResult(Player player, SpellPullResult result) {
        if (player == null || result == null || result.isEmpty()) {
            return;
        }
        sendSummary(player, "Unlocked", result.unlocked(), SummaryValue.COUNT);
        sendSummary(player, "Mastery gained", result.masteryGained(), SummaryValue.MASTERY);
        sendSummary(player, "Auto-discarded", result.autoDiscarded(), SummaryValue.COUNT);
        if (result.salvagedGems() > 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Auto-discarded maxed spell pulls for " + ChatColor.LIGHT_PURPLE
                            + result.salvagedGems() + " <glyph:purple_orb_icon>" + ChatColor.GREEN + ".");
        }
    }

    public static void sendSummary(Player player, String label, Map<SpellCardDefinition, Integer> pulls) {
        sendSummary(player, label, pulls, SummaryValue.COUNT);
    }

    private static void sendSummary(Player player,
                                    String label,
                                    Map<SpellCardDefinition, Integer> pulls,
                                    SummaryValue valueType) {
        if (player == null || pulls == null || pulls.isEmpty()) {
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, ChatColor.GRAY + label + ":");
        for (var entry : pulls.entrySet()) {
            SpellCardDefinition card = entry.getKey();
            int value = entry.getValue();
            String suffix = valueType == SummaryValue.MASTERY
                    ? ChatColor.GRAY + " +" + value + " mastery"
                    : ChatColor.GRAY + " x" + value;
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    ChatColor.DARK_GRAY + "- " + card.rarity().color() + card.displayName() + suffix);
        }
    }

    private enum SummaryValue {
        COUNT,
        MASTERY
    }
}

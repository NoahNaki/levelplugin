package me.nakilex.levelplugin.pet.utils;

import me.nakilex.levelplugin.pet.PetDefinition;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public final class PetPullSummaryUtil {
    private PetPullSummaryUtil() {
    }

    public static void sendSummary(Player player, String label, Map<PetDefinition, Integer> pulls) {
        if (player == null || pulls == null || pulls.isEmpty()) {
            return;
        }
        PetChatUtil.send(player, ChatColor.GRAY + label + ":");
        for (var entry : pulls.entrySet()) {
            String name = PetDisplayUtil.formatDisplayName(entry.getKey());
            int count = entry.getValue();
            PetChatUtil.send(player, ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + name
                    + ChatColor.GRAY + " x" + count);
        }
    }
}

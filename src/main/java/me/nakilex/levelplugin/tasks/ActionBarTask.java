package me.nakilex.levelplugin.tasks;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.listeners.ClickComboListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarTask extends BukkitRunnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

            double hp = player.getHealth();
            double maxHp = player.getMaxHealth();
            int currentMana = (int) ps.currentMana;
            int maxMana = ps.maxMana;

            // Get active combo
            String combo = ClickComboListener.getActiveCombo(player);
            String comboDisplay = combo.isEmpty() ? "" : formatCombo(combo, 3); // Hide if no active combo

            // Validate combo starter based on class
            String className = ps.playerClass.name().toLowerCase();
            if (!combo.isEmpty()) {
                if ((className.equals("archer") && !combo.startsWith("L")) || (!className.equals("archer") && !combo.startsWith("R"))) {
                    comboDisplay = ""; // Invalid combo start, hide display
                }
            }

            // Construct action bar message
            String leftText = String.format("§c%d/%d", (int) hp, (int) maxHp);
            String rightText = String.format("§b%d/%d", currentMana, maxMana);
            String message = String.format("%s%s%s", padRight(leftText, 10), centerText(comboDisplay, 10), padLeft(rightText, 10));

            // Send action bar
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }

    private String padRight(String text, int length) {
        return String.format("%-" + length + "s", text);
    }

    private String padLeft(String text, int length) {
        return String.format("%" + length + "s", text);
    }

    private String centerText(String text, int length) {
        int padding = (length - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, padding));
    }

    // New method to format combo string
    private String formatCombo(String combo, int maxLength) {
        if (combo.isEmpty()) return "";
        StringBuilder formatted = new StringBuilder("§7["); // Gray color
        int comboLength = combo.length();

        for (int i = 0; i < maxLength; i++) {
            if (i < comboLength) {
                formatted.append(combo.charAt(i));
            } else {
                formatted.append("_");
            }
        }
        formatted.append("]");

        return formatted.toString();
    }
}

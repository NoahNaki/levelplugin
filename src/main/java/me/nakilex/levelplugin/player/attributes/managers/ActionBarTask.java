package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.player.attributes.managers.ManaIndicatorManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarTask extends BukkitRunnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

            double hp = player.getHealth();
            double maxHp = player.getMaxHealth();
            int currentMana = (int) ps.currentMana;
            int maxMana = ps.maxMana;

            // Get active combo or mana cost display
            String combo = ClickComboListener.getActiveCombo(player);
            Integer manaCost = ManaIndicatorManager.getInstance().getCost(player);
            String centerDisplay = "";

            if (!combo.isEmpty()) {
                // Prioritize combo and clear mana indicator
                ManaIndicatorManager.getInstance().clear(player);
                String className = ps.playerClass.name().toLowerCase();
                if ((className.equals("archer") && !combo.startsWith("L")) || (!className.equals("archer") && !combo.startsWith("R"))) {
                    centerDisplay = ""; // Invalid combo start
                } else {
                    centerDisplay = formatCombo(combo, 3);
                }
            } else if (manaCost != null) {
                centerDisplay = formatCost(manaCost);
            }

            // Construct action bar message
            String leftText = String.format("§c%d/%d", (int) hp, (int) maxHp);
            String rightText = String.format("§b%d/%d", currentMana, maxMana);
            String message = String.format("%s%s%s", padRight(leftText, 10), centerText(centerDisplay, 20), padLeft(rightText, 10));

            // Send action bar
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }

    private static final java.util.regex.Pattern GLYPH_PATTERN = java.util.regex.Pattern.compile("<glyph:[^>]+>");

    private int visibleLength(String text) {
        String stripped = ChatColor.stripColor(text);
        stripped = GLYPH_PATTERN.matcher(stripped).replaceAll("?");
        return stripped.length();
    }

    private String padRight(String text, int length) {
        int padding = Math.max(0, length - visibleLength(text));
        return text + " ".repeat(padding);
    }

    private String padLeft(String text, int length) {
        int padding = Math.max(0, length - visibleLength(text));
        return " ".repeat(padding) + text;
    }

    private String centerText(String text, int length) {
        int padding = Math.max(0, length - visibleLength(text));
        int left = padding / 2;
        int right = padding - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    // New method to format combo string
    private String formatCombo(String combo, int maxLength) {
        if (combo.isEmpty()) return "";
        StringBuilder formatted = new StringBuilder();
        int comboLength = Math.min(combo.length(), maxLength);

        for (int i = 0; i < comboLength; i++) {
            char c = combo.charAt(i);
            if (c == 'R') {
                formatted.append("<glyph:right_mouse_click>");
            } else if (c == 'L') {
                formatted.append("<glyph:left_mouse_click>");
            } else {
                formatted.append(c);
            }
        }

        return formatted.toString();
    }

    private String formatCost(int cost) {
        return "§8[§b-" + cost + "§8]";
    }
}

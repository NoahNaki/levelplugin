package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.player.attributes.managers.ManaIndicatorManager;
import me.nakilex.levelplugin.utils.DefaultFontInfo;
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
            String message = padRightPx(trimToPx(leftText, LEFT_PX), LEFT_PX) +
                    centerTextPx(trimToPx(centerDisplay, CENTER_PX), CENTER_PX) +
                    padLeftPx(trimToPx(rightText, RIGHT_PX), RIGHT_PX);

            // Send action bar
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }

    private static final java.util.regex.Pattern GLYPH_PATTERN = java.util.regex.Pattern.compile("<glyph:[^>]+>");
    // Width in pixels of the custom combo glyphs. If this value is too small
    // the action bar segments will shift when the glyphs render at a larger
    // size. 8px keeps the layout stable with the current resource pack.
    private static final int GLYPH_PX = 10;
    private static final String NBSP = "\u00A0";

    private static final int LEFT_PX = 30;
    // Shrink the gap between HP and mana by ~30%
    private static final int CENTER_PX = 90;
    private static final int RIGHT_PX = 30;

    private int pixelLength(String text) {
        int px = 0;
        boolean code = false;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                code = true;
                continue;
            }
            if (code) {
                code = false;
                bold = c == 'l' || c == 'L';
                continue;
            }
            if (text.startsWith("<glyph:", i)) {
                int end = text.indexOf('>', i);
                if (end == -1) end = text.length() - 1;
                i = end;
                px += GLYPH_PX + 1;
                continue;
            }
            DefaultFontInfo fi = DefaultFontInfo.getDefaultFontInfo(c);
            px += (bold ? DefaultFontInfo.getBoldLength() : fi.getLength()) + 1;
        }
        return px;
    }

    private String repeatSpacePixels(int px) {
        int spacePx = DefaultFontInfo.SPACE.getLength() + 1;
        int count = (int) Math.ceil(Math.max(0, px) / (double) spacePx);
        return NBSP.repeat(count);
    }

    private String padRightPx(String text, int px) {
        int diff = px - pixelLength(text);
        return text + repeatSpacePixels(diff);
    }

    private String padLeftPx(String text, int px) {
        int diff = px - pixelLength(text);
        return repeatSpacePixels(diff) + text;
    }

    private String centerTextPx(String text, int px) {
        int diff = px - pixelLength(text);
        int left = diff / 2;
        int right = diff - left;
        return repeatSpacePixels(left) + text + repeatSpacePixels(right);
    }

    /**
     * Trim a string so that its visual width does not exceed the given pixel
     * count. This is aware of color codes and glyph placeholders to avoid
     * cutting them in half.
     */
    private String trimToPx(String text, int px) {
        while (!text.isEmpty() && pixelLength(text) > px) {
            int end = text.length() - 1;
            text = text.substring(0, end);

            // Remove trailing color code character if present
            if (text.endsWith("§")) {
                text = text.substring(0, text.length() - 1);
            }

            // Remove an unfinished glyph placeholder
            int start = text.lastIndexOf("<glyph:");
            if (start != -1 && text.indexOf('>', start) == -1) {
                text = text.substring(0, start);
            }
        }
        return text;
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
            // Add arrow separator between inputs
            if (i < comboLength - 1) {
                formatted.append("<glyph:small_arrow_right>");
            }
        }

        return formatted.toString();
    }

    private String formatCost(int cost) {
        return "§8[§b-" + cost + "§8]";
    }
}

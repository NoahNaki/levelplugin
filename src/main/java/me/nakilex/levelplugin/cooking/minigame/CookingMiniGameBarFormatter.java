package me.nakilex.levelplugin.cooking.minigame;

import org.bukkit.ChatColor;

/** Formats LiteCooking-style symbol bars for cooking mini-game title displays. */
public final class CookingMiniGameBarFormatter {
    private static final String SYMBOL = "◆";
    private static final ChatColor EMPTY_COLOR = ChatColor.DARK_GRAY;
    private static final ChatColor TARGET_COLOR = ChatColor.RED;
    private static final ChatColor HOOK_COLOR = ChatColor.GOLD;
    private static final ChatColor SUCCESS_COLOR = ChatColor.GREEN;
    private static final ChatColor LABEL_COLOR = ChatColor.GRAY;
    private static final ChatColor NUMBER_COLOR = ChatColor.WHITE;

    private CookingMiniGameBarFormatter() {
    }

    public static CookingMiniGameVisual hitBar(int hookIndex, int targetIndex, int barSize, int score, int targetScore, int health) {
        int safeBarSize = Math.max(1, barSize);
        int safeHook = clamp(hookIndex, 0, safeBarSize - 1);
        int safeTarget = clamp(targetIndex, 0, safeBarSize - 1);
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < safeBarSize; i++) {
            if (i == safeHook && i == safeTarget) {
                title.append(SUCCESS_COLOR).append(SYMBOL);
            } else if (i == safeHook) {
                title.append(HOOK_COLOR).append(SYMBOL);
            } else if (i == safeTarget) {
                title.append(TARGET_COLOR).append(SYMBOL);
            } else {
                title.append(EMPTY_COLOR).append(SYMBOL);
            }
        }
        String subtitle = LABEL_COLOR + "Score " + NUMBER_COLOR + score + LABEL_COLOR + "/" + NUMBER_COLOR + Math.max(1, targetScore)
                + LABEL_COLOR + "  Health " + NUMBER_COLOR + Math.max(0, health);
        return new CookingMiniGameVisual(title.toString(), subtitle);
    }

    public static CookingMiniGameVisual mixBar(int clicks, int requiredClicks, int barSize) {
        int safeRequired = Math.max(1, requiredClicks);
        int safeClicks = Math.max(0, clicks);
        int safeBarSize = Math.max(1, barSize);
        int progressSlots = (int) Math.round((Math.min(safeClicks, safeRequired) / (double) safeRequired) * safeBarSize);
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < safeBarSize; i++) {
            title.append(i < progressSlots ? HOOK_COLOR : EMPTY_COLOR).append(SYMBOL);
        }
        String subtitle = NUMBER_COLOR + String.valueOf(safeClicks) + LABEL_COLOR + "/" + NUMBER_COLOR + safeRequired;
        return new CookingMiniGameVisual(title.toString(), subtitle);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

package me.nakilex.levelplugin.cooking.minigame;

import org.bukkit.ChatColor;

/** Formats LiteCooking-style symbol bars for cooking mini-game title displays. */
public final class CookingMiniGameBarFormatter {
    private static final String DEFAULT_HIT_SYMBOL = "●";
    private static final String DEFAULT_MIX_SYMBOL = "🔥";
    private static final int HIT_PROGRESS_DOTS = 10;
    private static final ChatColor LINE_COLOR = ChatColor.DARK_GRAY;
    private static final ChatColor TARGET_COLOR = ChatColor.RED;
    private static final ChatColor HOOK_COLOR = ChatColor.GOLD;
    private static final ChatColor MIX_MIDDLE_COLOR = ChatColor.GOLD;
    private static final ChatColor SUCCESS_COLOR = ChatColor.GREEN;
    private static final ChatColor HEALTH_COLOR = ChatColor.GOLD;
    private static final ChatColor PROGRESS_COLOR = ChatColor.LIGHT_PURPLE;
    private static final ChatColor EMPTY_PROGRESS_COLOR = ChatColor.DARK_GRAY;

    private CookingMiniGameBarFormatter() {
    }

    public static CookingMiniGameVisual hitBar(int hookIndex,
                                               int targetIndex,
                                               int barSize,
                                               int score,
                                               int targetScore,
                                               int health,
                                               String targetSymbol,
                                               String hookSymbol,
                                               String lineSymbol,
                                               String healthSymbol) {
        int safeBarSize = Math.max(1, barSize);
        int safeHook = clamp(hookIndex, 0, safeBarSize - 1);
        int safeTarget = clamp(targetIndex, 0, safeBarSize - 1);
        String safeTargetSymbol = configuredSymbol(targetSymbol, DEFAULT_HIT_SYMBOL);
        String safeHookSymbol = configuredSymbol(hookSymbol, DEFAULT_HIT_SYMBOL);
        String safeLineSymbol = configuredSymbol(lineSymbol, DEFAULT_HIT_SYMBOL);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < safeBarSize; i++) {
            if (i == safeHook && i == safeTarget) {
                bar.append(SUCCESS_COLOR).append(safeHookSymbol);
            } else if (i == safeHook) {
                bar.append(HOOK_COLOR).append(safeHookSymbol);
            } else if (i == safeTarget) {
                bar.append(TARGET_COLOR).append(safeTargetSymbol);
            } else {
                bar.append(LINE_COLOR).append(safeLineSymbol);
            }
        }
        String safeHealthSymbol = configuredSymbol(healthSymbol, DEFAULT_MIX_SYMBOL);
        int safeHealth = Math.max(0, health);
        int leftHealth = (int) Math.ceil(safeHealth / 2.0D);
        int rightHealth = safeHealth / 2;
        String title = repeatColored(HEALTH_COLOR, safeHealthSymbol, leftHealth)
                + (leftHealth > 0 ? " " : "")
                + bar
                + (rightHealth > 0 ? " " : "")
                + repeatColored(HEALTH_COLOR, safeHealthSymbol, rightHealth);
        return new CookingMiniGameVisual(title, hitSubtitle(score, targetScore));
    }

    public static CookingMiniGameVisual mixBar(int clicks,
                                               int requiredClicks,
                                               int barSize,
                                               String filledSymbol,
                                               String emptySymbol) {
        int safeRequired = Math.max(1, requiredClicks);
        int safeClicks = Math.max(0, clicks);
        int safeBarSize = Math.max(1, barSize);
        int progressSlots = (int) Math.round((Math.min(safeClicks, safeRequired) / (double) safeRequired) * safeBarSize);
        String safeFilledSymbol = configuredSymbol(filledSymbol, DEFAULT_MIX_SYMBOL);
        String safeEmptySymbol = configuredSymbol(emptySymbol, DEFAULT_MIX_SYMBOL);
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < safeBarSize; i++) {
            title.append(i < progressSlots ? MIX_MIDDLE_COLOR : LINE_COLOR)
                    .append(i < progressSlots ? safeFilledSymbol : safeEmptySymbol);
        }
        return new CookingMiniGameVisual(title.toString(), "");
    }

    private static String hitSubtitle(int score, int targetScore) {
        int safeTargetScore = Math.max(1, targetScore);
        int filledDots = (int) Math.round((Math.min(Math.max(0, score), safeTargetScore) / (double) safeTargetScore) * HIT_PROGRESS_DOTS);
        StringBuilder subtitle = new StringBuilder();
        for (int i = 0; i < HIT_PROGRESS_DOTS; i++) {
            subtitle.append(i < filledDots ? PROGRESS_COLOR : EMPTY_PROGRESS_COLOR).append(DEFAULT_HIT_SYMBOL);
        }
        return subtitle.toString();
    }

    private static String repeatColored(ChatColor color, String symbol, int amount) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            builder.append(color).append(symbol);
        }
        return builder.toString();
    }

    private static String configuredSymbol(String configured, String fallback) {
        return configured == null || configured.isBlank() ? fallback : configured;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

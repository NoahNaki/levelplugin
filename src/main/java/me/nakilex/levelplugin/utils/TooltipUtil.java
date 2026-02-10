package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.items.data.ItemRarity;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper methods for formatting item tooltips. Provides lightweight utilities
 * such as generating progress bars that can be reused across many GUI classes.
 */
public final class TooltipUtil {

    private TooltipUtil() {}

    /**
     * Create a textual progress bar for the given values using the standard
     * colours defined in {@link GuiUtil}.
     *
     * @param current the current value
     * @param max     the maximum value represented by a full bar
     * @param length  number of characters in the bar
     * @return coloured progress bar string
     */
    public static String progressBar(double current, double max, int length) {
        double progress = max <= 0 ? 0.0 : current / max;
        return GuiUtil.createProgressBar(progress, length);
    }

    private static int strikethroughSpacePixelWidth() {
        String unit = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + " " + ChatColor.RESET;
        return Math.max(1, ChatFormatter.pixelLength(unit));
    }

    /**
     * Create a strikethrough-styled progress bar intended for experience style progress displays.
     *
     * @param current current progress value
     * @param max maximum progress value
     * @param length number of strikethrough characters
     * @return coloured strikethrough progress bar
     */
    public static String expProgressBar(double current, double max, int length) {
        int safeLength = Math.max(1, length);
        double ratio = max <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, current / max));
        int filled = (int) Math.round(ratio * safeLength);
        filled = Math.max(0, Math.min(safeLength, filled));
        int empty = safeLength - filled;

        StringBuilder sb = new StringBuilder();
        if (filled > 0) {
            sb.append(ChatColor.GREEN).append(ChatColor.STRIKETHROUGH).append(" ".repeat(filled));
        }
        if (empty > 0) {
            sb.append(ChatColor.DARK_GRAY).append(ChatColor.STRIKETHROUGH).append(" ".repeat(empty));
        }
        sb.append(ChatColor.RESET);
        return sb.toString();
    }

    /**
     * Create a strikethrough-styled experience bar that targets a visual width in pixels.
     */
    public static String expProgressBarByPixels(double current, double max, int pixelWidth) {
        int target = Math.max(80, pixelWidth);
        int segments = (int) Math.ceil(target / (double) strikethroughSpacePixelWidth());
        segments = Math.max(16, Math.min(34, segments));
        return expProgressBar(current, max, segments);
    }

    /**
     * Generate standard left/right click instruction lines.
     *
     * @param leftAction  description following "Left-click" or {@code null}
     * @param rightAction description following "Right-click" or {@code null}
     * @return list of formatted instruction lines
     */
    public static List<String> clickInstructions(String leftAction, String rightAction) {
        List<String> lore = new ArrayList<>(2);
        if (leftAction != null) {
            lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + leftAction);
        }
        if (rightAction != null) {
            lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + rightAction);
        }
        return lore;
    }

    /**
     * Generate standard sneak + click instruction lines. This mirrors the base click instruction
     * styling while calling out the sneak modifier for clarity.
     *
     * @param leftAction  description following "Sneak + Left-click" or {@code null}
     * @param rightAction description following "Sneak + Right-click" or {@code null}
     * @return list of formatted instruction lines
     */
    public static List<String> sneakClickInstructions(String leftAction, String rightAction) {
        List<String> lore = new ArrayList<>(2);
        if (leftAction != null) {
            lore.add(ChatColor.WHITE + "Sneak + Left-click " + ChatColor.GRAY + leftAction);
        }
        if (rightAction != null) {
            lore.add(ChatColor.WHITE + "Sneak + Right-click " + ChatColor.GRAY + rightAction);
        }
        return lore;
    }

    /**
     * Generate a coloured bullet list using the standard grey styling. This is useful for
     * describing key points in GUI tooltips without hand-writing the prefix every time.
     *
     * @param entries description lines to include
     * @return list of formatted bullet lines
     */
    public static List<String> bulletList(String... entries) {
        List<String> lore = new ArrayList<>();
        if (entries == null) {
            return lore;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + entry.trim());
        }
        return lore;
    }

    /**
     * Generate a standard selection line for filter/sort menus.
     *
     * @param selected whether this option is the active selection
     * @param label    display text for the option
     * @return formatted selection line
     */
    public static String selectionLine(boolean selected, String label) {
        ChatColor color = selected ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = selected ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
    }


    /**
     * Wrap a lore line to a max pixel width using Minecraft font metrics.
     *
     * @param line      source lore line
     * @param maxPixels maximum pixel width for a line
     * @return wrapped lore lines
     */
    public static List<String> wrapLoreLine(String line, int maxPixels) {
        return wrapLoreLine(line, maxPixels, ChatColor.GRAY.toString());
    }

    /**
     * Wrap a lore line to a max pixel width using Minecraft font metrics,
     * with a custom prefix for continuation lines.
     *
     * @param line               source lore line
     * @param maxPixels          maximum pixel width for a line
     * @param continuationPrefix prefix used for continuation lines
     * @return wrapped lore lines
     */
    public static List<String> wrapLoreLine(String line, int maxPixels, String continuationPrefix) {
        List<String> wrapped = new ArrayList<>();
        if (line == null || line.isBlank()) {
            return wrapped;
        }
        int targetPixels = Math.max(80, maxPixels);
        String[] words = line.trim().split("\\s+");
        String current = "";
        String continuation = continuationPrefix == null ? "" : continuationPrefix;
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (ChatFormatter.pixelLength(candidate) <= targetPixels) {
                current = candidate;
                continue;
            }
            if (!current.isEmpty()) {
                wrapped.add(current);
                String carryColor = ChatColor.getLastColors(current);
                current = (continuation + carryColor + word).trim();
                continue;
            }
            wrapped.add(word);
        }
        if (!current.isEmpty()) {
            wrapped.add(current);
        }
        return wrapped;
    }


    /**
     * Build a lightweight strikethrough divider line for lore sections.
     *
     * @param spaces number of strikethrough spaces to render
     * @return formatted divider line
     */
    public static String sectionDivider(int spaces) {
        int width = Math.max(10, spaces);
        return ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + " ".repeat(width) + ChatColor.RESET;
    }

    /**
     * Build a strikethrough divider targeting a visual lore width in pixels.
     *
     * @param pixelWidth target width in pixels
     * @return formatted divider line
     */
    public static String sectionDividerByPixels(int pixelWidth) {
        int target = Math.max(80, pixelWidth);
        int spaces = (int) Math.ceil(target / (double) strikethroughSpacePixelWidth());
        spaces = Math.max(16, Math.min(34, spaces));
        return sectionDivider(spaces);
    }

    /**
     * Build a lore-aware section divider based on current visible lines.
     */
    public static String sectionDividerForLore(List<String> loreLines) {
        int width = 0;
        if (loreLines != null) {
            for (String line : loreLines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                width = Math.max(width, ChatFormatter.pixelLength(line));
            }
        }
        width = Math.max(140, Math.min(190, width + 10));
        return sectionDividerByPixels(width);
    }

    /**
     * Build a standard strikethrough divider width for lore sections.
     *
     * @return formatted divider line
     */
    public static String sectionDivider() {
        return sectionDividerByPixels(170);
    }

    /**
     * Generate standard lore for quest items so they share the same divider and
     * label styling everywhere.
     *
     * @param description optional flavor text shown before the divider
     * @param soulbound   whether to append the red soulbound line
     * @return list of formatted lore lines
     */
    public static List<String> questItemLore(String description, boolean soulbound) {
        List<String> lore = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            lore.add(ChatColor.GRAY + description.trim());
        }
        lore.add("");
        lore.add(ChatColor.WHITE + "Quest Item");
        if (soulbound) {
            lore.add(ChatColor.RED + "Soulbound");
        }
        return lore;
    }

    /**
     * Generate standard lore for dungeon items so they follow the same divider
     * styling as quest items while carrying the dungeon-specific label.
     *
     * @param description optional flavour text shown before the divider
     * @param soulbound   whether to append the red soulbound line
     * @return list of formatted lore lines
     */
    public static List<String> dungeonItemLore(String description, boolean soulbound) {
        List<String> lore = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            lore.add(ChatColor.GRAY + description.trim());
        }
        lore.add("");
        lore.add(ChatColor.WHITE + "Dungeon Item");
        if (soulbound) {
            lore.add(ChatColor.RED + "Soulbound");
        }
        return lore;
    }

    /**
     * Standard account limit line for shop tooltips to keep styling consistent.
     *
     * @param limit maximum purchases per account
     * @return formatted lore line, e.g. "§7Account Limit: §f1"
     */
    public static String accountLimitLine(int limit) {
        return ChatColor.GRAY + "Account Limit: " + ChatColor.WHITE + limit;
    }

    /**
     * Format a section header for tooltips using the standard gold styling.
     *
     * @param text header label
     * @return formatted header line
     */
    public static String sectionHeader(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return ChatColor.GOLD + "" + ChatColor.BOLD + text.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Format an arrow-prefixed line using the standard gold arrow symbol.
     *
     * @param line content to append after the arrow
     * @return formatted arrow line
     */
    public static String arrowLine(String line) {
        if (line == null) {
            return "";
        }
        return ChatColor.GOLD + "» " + line;
    }

    /**
     * Format a stat line with a label and colored value using the standard arrow prefix.
     *
     * @param label      stat label
     * @param value      value text
     * @param valueColor color for the value
     * @return formatted stat line
     */
    public static String statLine(String label, String value, ChatColor valueColor) {
        if (label == null || label.isBlank()) {
            return "";
        }
        String safeValue = value == null ? "" : value;
        ChatColor valueShade = valueColor == null ? ChatColor.WHITE : valueColor;
        return arrowLine(ChatColor.YELLOW + label.trim() + ChatColor.GRAY + ": " + valueShade + safeValue);
    }

    /**
     * Format a leaderboard line with a rank, name, and value.
     *
     * @param rank  ranking position (1-indexed)
     * @param name  player name
     * @param value numeric value to display
     * @param label trailing label (e.g., "Kills")
     * @return formatted leaderboard line
     */
    public static String leaderboardLine(int rank, String name, String value, String label) {
        ChatColor rankColor = switch (rank) {
            case 1 -> ChatColor.GOLD;
            case 2 -> ChatColor.GRAY;
            case 3 -> ChatColor.DARK_GRAY;
            default -> ChatColor.GRAY;
        };
        String safeName = (name == null || name.isBlank()) ? "Unknown" : name;
        String safeValue = value == null ? "" : value;
        String safeLabel = (label == null || label.isBlank()) ? "" : " " + label;
        return rankColor + "#" + rank + " " + ChatColor.YELLOW + safeName
                + ChatColor.GRAY + " " + ChatColor.WHITE + safeValue + ChatColor.GRAY + safeLabel;
    }

    /**
     * Format a rarity glyph line using the standard rarity color and symbol.
     *
     * @param rarity item rarity to format
     * @return formatted glyph line
     */
    public static String rarityLine(ItemRarity rarity) {
        if (rarity == null) {
            rarity = ItemRarity.COMMON;
        }
        String name = rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase(Locale.ROOT);
        return rarity.getColor() + rarity.getSymbol() + ChatColor.GRAY + " " + rarity.getColor() + name;
    }

    /**
     * Center only the display name of an item's tooltip.
     *
     * @param item item to update
     */
    public static void centerItemName(ItemStack item) {
        TextUtil.centerItemTooltip(item, true, false);
    }
}

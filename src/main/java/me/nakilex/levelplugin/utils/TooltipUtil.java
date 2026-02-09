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

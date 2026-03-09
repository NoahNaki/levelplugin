package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoItems;
import me.nakilex.levelplugin.Main;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Utility helpers for basic GUI elements. */
public final class GuiUtil {
    private GuiUtil() {}
    private static final Pattern GLYPH_PATTERN = Pattern.compile("<glyph:[^>]+>");

    /**
     * Standard 28-slot layout used by paginated menus. The pattern spans
     * four centered rows of seven slots each and leaves a border around the
     * edges for navigation controls.
     */
    public static final int[] PAGED_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    /** Create a simple filler pane with a blank display name. */
    public static ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Build a Nexo item with a custom name or a barrier if missing. */
    public static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        ItemStack item = builder != null ? builder.build() : new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Build a Nexo item with a custom name, lore, and hidden attributes. */
    public static ItemStack getNexoItem(String id, String name, List<String> lore) {
        ItemStack item = getNexoItem(id, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Build a standard GUI item with the provided display name and lore. */
    public static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Build a rarity arrow item using the salvage GUI arrow icons.
     */
    public static ItemStack getRarityArrowItem(ItemRarity rarity, String name) {
        return getNexoItem(getRarityArrowId(rarity), name);
    }

    /** Build a rarity-themed pet icon item using pet paw glyph models. */
    public static ItemStack getRarityPetIconItem(ItemRarity rarity, String name, List<String> lore) {
        return getNexoItem(getRarityPetIconId(rarity), name, lore);
    }

    /** Map an item rarity to a Nexo pet icon id. */
    public static String getRarityPetIconId(ItemRarity rarity) {
        return "pet_icon_" + normalizeRarityToken(rarity);
    }

    /**
     * Map an item rarity to a Nexo arrow icon id.
     */
    public static String getRarityArrowId(ItemRarity rarity) {
        return "arrow_" + normalizeRarityToken(rarity);
    }

    private static String normalizeRarityToken(ItemRarity rarity) {
        if (rarity == null) {
            return "common";
        }
        return switch (rarity) {
            case COMMON -> "common";
            case UNCOMMON -> "uncommon";
            case RARE -> "rare";
            case EPIC -> "epic";
            case LEGENDARY -> "legendary";
            case MYTHIC, FABLED -> "mythic";
        };
    }

    /**
     * Create a standardized toggle item using check/cross Nexo icons and lore
     * indicating the current enabled status.
     *
     * @param enabled whether the feature is enabled
     * @param name    display name of the item
     * @param extraLore optional additional lore lines (e.g. instructions)
     * @return configured ItemStack representing the toggle state
     */
    public static ItemStack createToggleItem(boolean enabled, String name, String... extraLore) {
        ItemStack base = getNexoItem(enabled ? "check" : "cross", name);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"));
            if (extraLore != null && extraLore.length > 0) {
                lore.add(" ");
                for (String line : extraLore) {
                    lore.add(line);
                }
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            base.setItemMeta(meta);
        }
        return base;
    }

    /**
     * Fill the outer border of an inventory with the given filler item.
     * This is reused by multiple GUI classes.
     */
    public static void fillBorder(org.bukkit.inventory.Inventory inv, ItemStack filler) {
        int size = inv.getSize();
        int cols = 9;
        int rows = size / cols;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slot = row * cols + col;
                if (row == 0 || row == rows - 1 || col == 0 || col == cols - 1) {
                    inv.setItem(slot, filler);
                }
            }
        }
    }

    /**
     * Generate a star rating string using the provided characters.
     *
     * @param value number of filled stars
     * @param max   maximum number of stars to display
     * @param filled character(s) to use for a filled star
     * @param empty  character(s) to use for an empty star
     * @return star string of length {@code max}
     */
    public static String generateStars(int value, int max, String filled, String empty) {
        StringBuilder sb = new StringBuilder(max * Math.max(filled.length(), empty.length()));
        for (int i = 0; i < value && i < max; i++) sb.append(filled);
        for (int i = value; i < max; i++) sb.append(empty);
        return sb.toString();
    }

    /** Convenience wrapper using "✦" and "✧" characters. */
    public static String generateStars(int value, int max) {
        return generateStars(value, max, "✦", "✧");
    }

    /** Generate star glyphs repeated the given number of times. */
    public static String glyphStars(int count) {
        if (count <= 0) return "";
        return "<glyph:star>".repeat(count);
    }

    /** Format a stat name using standard colouring without icons. */
    public static String formatStatName(StatType type) {
        return ChatColor.GRAY + type.getDisplayName();
    }

    /** Format a stat line using standard colours without icons. */
    public static String formatStatLine(StatType type, int value, boolean percent) {
        String suffix = percent ? "%" : "";
        ChatColor valueColor = (type == StatType.VIT) ? ChatColor.RED : ChatColor.GREEN;
        return formatStatName(type) + ": " + valueColor + "+" + value + suffix;
    }

    /**
     * Create a textual progress bar using repeated characters.
     *
     * @param progress value between 0.0 and 1.0
     * @param length   number of characters in the bar
     * @param filled   color for the filled portion
     * @param empty    color for the empty portion
     * @param symbol   character to repeat for the bar segments
     * @return colored progress bar string
     */
    public static String createProgressBar(double progress, int length, ChatColor filled,
                                           ChatColor empty, String symbol) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        int filledLen = (int) Math.round(progress * length);
        StringBuilder sb = new StringBuilder(length * symbol.length());
        sb.append(filled).append(symbol.repeat(Math.max(0, filledLen)));
        sb.append(empty).append(symbol.repeat(Math.max(0, length - filledLen)));
        return sb.toString();
    }

    /** Convenience wrapper using green/white colors and '-' characters. */
    public static String createProgressBar(double progress, int length) {
        return createProgressBar(progress, length, ChatColor.GREEN, ChatColor.WHITE, "-");
    }

    /**
     * Normalize inventory titles by stripping color codes and glyph placeholders.
     */
    public static String normalizeTitle(String title) {
        if (title == null) return "";
        String stripped = ChatColor.stripColor(title);
        return GLYPH_PATTERN.matcher(stripped).replaceAll("").trim();
    }

    /** Compare titles after normalization to avoid glyph/color mismatches. */
    public static boolean titleMatches(String title, String expected) {
        return normalizeTitle(title).equalsIgnoreCase(normalizeTitle(expected));
    }

    /** Return from a custom GUI to the player's inventory view. */
    public static void openPlayerInventory(Player player) {
        if (player == null) {
            return;
        }
        player.closeInventory();
        Main main = Main.getInstance();
        if (main == null) {
            player.openInventory(player.getInventory());
            return;
        }
        Bukkit.getScheduler().runTaskLater(main, () -> {
            if (player.isOnline()) {
                player.openInventory(player.getInventory());
            }
        }, 1L);
    }

    /** Compare normalized titles by prefix, useful for dynamic counters in GUI titles. */
    public static boolean titleStartsWith(String title, String expectedPrefix) {
        return normalizeTitle(title).startsWith(normalizeTitle(expectedPrefix));
    }
}

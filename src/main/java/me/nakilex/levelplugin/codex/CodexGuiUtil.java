package me.nakilex.levelplugin.codex;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Utility helpers for codex GUIs. */
public final class CodexGuiUtil {
    private CodexGuiUtil() {}

    /** Slots used for content in paged codex inventories. */
    public static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    /**
     * Create the info book summarising codex discoveries.
     * @param title book display name
     * @param lines mapping of category name to value string (e.g., "Mobs" -> "1/5")
     */
    public static ItemStack createInfoBook(String title, Map<String, String> lines) {
        return createInfoBook(title, lines, (String[]) null);
    }

    /**
     * Create the info book summarising codex discoveries with extra lore lines.
     *
     * @param title book display name
     * @param lines mapping of category name to value string
     * @param extraLore optional additional lore lines appended after a blank line
     */
    public static ItemStack createInfoBook(String title, Map<String, String> lines, String... extraLore) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + title);
            List<String> lore = new ArrayList<>();
            for (Map.Entry<String, String> entry : lines.entrySet()) {
                lore.add(ChatColor.GRAY + entry.getKey() + ": " + ChatColor.WHITE + entry.getValue());
            }
            if (extraLore != null && extraLore.length > 0) {
                lore.add(" ");
                for (String line : extraLore) lore.add(line);
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            book.setItemMeta(meta);
        }
        return book;
    }
}

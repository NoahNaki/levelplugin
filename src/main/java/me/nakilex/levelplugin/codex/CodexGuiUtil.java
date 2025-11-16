package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Utility helpers for codex GUIs. */
public final class CodexGuiUtil {
    private CodexGuiUtil() {}

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

    /**
     * Build a standardised lore block describing the player's Codex progress for a given mob.
     *
     * @param manager  codex manager providing discovery data
     * @param playerId player identifier to inspect
     * @param key      mob identifier stored in the codex
     * @return lore lines describing level, kills and progress towards the next level
     */
    public static List<String> mobProgressLore(CodexManager manager, UUID playerId, String key) {
        List<String> lore = new ArrayList<>();
        if (manager == null || playerId == null || key == null || key.isBlank()) {
            return lore;
        }

        int level = manager.getMobLevel(playerId, key);
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + level);

        int kills = manager.getKillCount(playerId, key);
        if (level >= manager.getMaxMobLevel()) {
            String bar = TooltipUtil.progressBar(1, 1, 15);
            lore.add(bar + " " + ChatColor.YELLOW + kills + ChatColor.GOLD + "+");
        } else {
            int next = manager.getKillsForLevel(level + 1);
            String bar = TooltipUtil.progressBar(kills, next, 15);
            lore.add(bar + " " + ChatColor.YELLOW + kills + ChatColor.GOLD + "/" + ChatColor.YELLOW + next);
        }
        return lore;
    }
}

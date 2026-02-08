package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
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

        int kills = manager.getKillCountForIdentity(playerId, key);
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

    /**
     * Build the detailed tooltip for a mob entry in the codex.
     */
    public static List<String> mobCodexLore(CodexManager manager, UUID playerId, String key) {
        List<String> lore = new ArrayList<>();
        if (manager == null || playerId == null || key == null || key.isBlank()) {
            return lore;
        }

        int kills = manager.getKillCountForIdentity(playerId, key);
        lore.add(ChatColor.GRAY + "You killed this mob " + ChatColor.WHITE
                + NumberUtil.formatCommas(kills) + "x" + ChatColor.GRAY + " on this profile.");
        lore.add(" ");

        List<String> stats = mobStatsLore(manager, key);
        if (stats.isEmpty()) {
            lore.add(TooltipUtil.arrowLine(ChatColor.DARK_GRAY + "Stats unavailable."));
        } else {
            lore.addAll(stats);
        }

        lore.add(" ");
        lore.add(TooltipUtil.sectionHeader("Next Milestone"));
        int level = manager.getMobLevel(playerId, key);
        if (level >= manager.getMaxMobLevel()) {
            lore.add(TooltipUtil.arrowLine(ChatColor.WHITE + NumberUtil.formatCommas(kills)
                    + ChatColor.GOLD + "/MAX " + ChatColor.GOLD + "<glyph:coins_icon> "
                    + ChatColor.WHITE + "0"));
        } else {
            int next = manager.getKillsForLevel(level + 1);
            int coins = manager.getMilestoneCoinReward(key, level + 1);
            lore.add(TooltipUtil.arrowLine(ChatColor.WHITE + NumberUtil.formatCommas(kills)
                    + ChatColor.GOLD + "/" + ChatColor.WHITE + NumberUtil.formatCommas(next)
                    + ChatColor.GOLD + " <glyph:coins_icon> " + ChatColor.WHITE
                    + NumberUtil.formatCommas(coins)));
        }

        lore.add(" ");
        String mobName = resolveMobName(manager, key);
        lore.add(ChatColor.GREEN + "Most " + mobName + " Kills:");
        List<CodexManager.MobKillEntry> top = manager.getTopMobKillers(key, 3);
        for (int i = 0; i < 3; i++) {
            if (i < top.size()) {
                CodexManager.MobKillEntry entry = top.get(i);
                lore.add(TooltipUtil.leaderboardLine(
                        i + 1,
                        entry.playerName(),
                        NumberUtil.formatCommas(entry.kills()),
                        "Kills"));
            } else {
                lore.add(TooltipUtil.leaderboardLine(
                        i + 1,
                        "None",
                        "0",
                        "Kills"));
            }
        }

        return lore;
    }

    public static String resolveMobName(CodexManager manager, String key) {
        CustomMobDefinition def = manager.getCustomMobDefinition(key).orElse(null);
        if (def != null) {
            String stripped = ChatColor.stripColor(def.displayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }
        return MobNameUtil.getPlainDisplayName(key);
    }

    private static List<String> mobStatsLore(CodexManager manager, String key) {
        List<String> lines = new ArrayList<>();
        CustomMobDefinition def = manager.getCustomMobDefinition(key).orElse(null);
        if (def == null) {
            return lines;
        }
        CustomMobDefinition.CustomMobAttributes attrs = def.computeAttributes();
        lines.add(TooltipUtil.statLine("Health", formatDouble(attrs.maxHealth()), ChatColor.WHITE));
        if (attrs.attackDamage() != null) {
            lines.add(TooltipUtil.statLine("Damage", formatDouble(attrs.attackDamage()), ChatColor.WHITE));
        }
        lines.add(TooltipUtil.statLine("Movement Speed", formatDouble(attrs.movementSpeed()), ChatColor.WHITE));
        if (attrs.attackSpeed() != null) {
            lines.add(TooltipUtil.statLine("Attack Speed", formatDouble(attrs.attackSpeed()), ChatColor.WHITE));
        }
        if (attrs.knockbackResistance() != null) {
            lines.add(TooltipUtil.statLine("Knockback Resist", formatDouble(attrs.knockbackResistance()), ChatColor.WHITE));
        }
        return lines;
    }

    private static String formatDouble(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}

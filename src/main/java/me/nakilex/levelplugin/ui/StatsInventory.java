package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.managers.StatsManager.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class StatsInventory {

    public static Inventory getStatsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.BLUE + "Allocate Your Stats");

        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

        // Each stat book now shows incremental and total effects
        inv.setItem(0, createStatBook(
            "STR", StatType.STR, ps.strength, ps.skillPoints,
            "Increases melee damage.",
            new String[]{
                "Each point: +0.5 melee damage",
                "Current bonus: " + ps.strength * 0.5 + " damage"
            }
        ));
        inv.setItem(1, createStatBook(
            "AGI", StatType.AGI, ps.agility, ps.skillPoints,
            "Increases movement speed & dodge chance.",
            new String[]{
                "Each point: +1% dodge, +0.001 walk speed",
                "Current dodge: " + ps.agility + "%, Speed: +" + (ps.agility * 0.001f)
            }
        ));
        inv.setItem(2, createStatBook(
            "INT", StatType.INT, ps.intelligence, ps.skillPoints,
            "Increases max mana & mana regen.",
            new String[]{
                "Each point: +10 max mana, +0.05 mana/sec",
                "Current max mana: " + ps.maxMana + ", Regen: " + (ps.intelligence * 0.05) + " mana/sec"
            }
        ));
        inv.setItem(3, createStatBook(
            "DEX", StatType.DEX, ps.dexterity, ps.skillPoints,
            "Increases crit chance.",
            new String[]{
                "Each point: +1% crit chance",
                "Current crit chance: " + ps.dexterity + "%"
            }
        ));
        inv.setItem(4, createStatBook(
            "HP", StatType.HP, ps.healthStat, ps.skillPoints,
            "Increases max health.",
            new String[]{
                "Each point: +2 max HP",
                "Current HP bonus: " + (ps.healthStat * 2) + " HP"
            }
        ));
        inv.setItem(5, createStatBook(
            "DEF", StatType.DEF, ps.defenceStat, ps.skillPoints,
            "Reduces incoming damage.",
            new String[]{
                "Each point: 0.5 damage negation",
                "Current negation: " + (ps.defenceStat * 0.5) + " damage"
            }
        ));

        // Player head with overall summary
        inv.setItem(8, createPlayerHead(player, ps));

        return inv;
    }

    /**
     * Create a stat book with extended incremental and total effect details.
     */
    private static ItemStack createStatBook(
        String statName,
        StatType statType,
        int currentValue,
        int skillPoints,
        String description,
        String[] effectDetails
    ) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(ChatColor.GREEN + statName);
        meta.setAuthor("System");
        meta.setDisplayName(ChatColor.YELLOW + statName + " Book");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add(ChatColor.DARK_AQUA + "Currently: " + currentValue);
        lore.add(ChatColor.DARK_PURPLE + "Unspent Skill Points: " + skillPoints);
        lore.add("");
        // Add incremental and total effect lines
        for (String line : effectDetails) {
            lore.add(ChatColor.GOLD + line);
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "Left-click: +1 " + statName);
        lore.add(ChatColor.RED + "Right-click: -1 " + statName);

        meta.setLore(lore);
        meta.setPages("Invest or refund 1 point in " + statName + ".");
        book.setItemMeta(meta);

        return book;
    }

    /**
     * Player head displaying overall stats summary.
     */
    private static ItemStack createPlayerHead(Player player, PlayerStats ps) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(ChatColor.GOLD + player.getName() + "'s Stats");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Health: " + (int) player.getHealth() + "/" + (int) player.getMaxHealth());
        lore.add(ChatColor.BLUE + "Mana: " + (int)ps.currentMana + "/" + ps.maxMana);
        lore.add(ChatColor.RED + "STR: " + ps.strength);
        lore.add(ChatColor.GREEN + "AGI: " + ps.agility);
        lore.add(ChatColor.LIGHT_PURPLE + "INT: " + ps.intelligence);
        lore.add(ChatColor.AQUA + "DEX: " + ps.dexterity);
        lore.add(ChatColor.GRAY + "DEF: " + ps.defenceStat);
        lore.add(ChatColor.DARK_AQUA + "SkillPoints Left: " + ps.skillPoints);

        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);
        return head;
    }
}

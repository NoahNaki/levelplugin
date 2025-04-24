package me.nakilex.levelplugin.player.attributes.gui;

import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsInventory {

    public static Inventory getStatsMenu(Player player) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(
            null,
            27,
            ps.skillPoints + " skill points remaining"
        );

        // Stat books with base and bonus stats
        inv.setItem(10, createStatBook(
            "Strength", StatType.STR, ps.baseStrength, ps.bonusStrength, ps.skillPoints,
            "Increases your melee damage.",
            new String[]{
                "Each point increases melee damage by 0.5.",
                "Current bonus: " + ChatColor.YELLOW + ((ps.baseStrength + ps.bonusStrength) * 0.5) + " damage."
            }
        ));
        inv.setItem(11, createStatBook(
            "Agility", StatType.AGI, ps.baseAgility, ps.bonusAgility, ps.skillPoints,
            "Improves your speed and dodge chance.",
            new String[]{
                "Each point increases dodge chance by 1% and speed by 0.001.",
                "Current dodge chance: " + ChatColor.YELLOW + (ps.baseAgility + ps.bonusAgility) + "%.",
                "Speed bonus: +" + ((ps.baseAgility + ps.bonusAgility) * 0.001f)
            }
        ));
        inv.setItem(12, createStatBook(
            "Intelligence", StatType.INT, ps.baseIntelligence, ps.bonusIntelligence, ps.skillPoints,
            "Increases your max mana and mana regeneration.",
            new String[]{
                "Each point adds 10 max mana and 0.05 mana/sec.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana,
                "Mana regen: +" + ((ps.baseIntelligence + ps.bonusIntelligence) * 0.05) + " mana/sec."
            }
        ));
        inv.setItem(14, createStatBook(
            "Dexterity", StatType.DEX, ps.baseDexterity, ps.bonusDexterity, ps.skillPoints,
            "Improves your critical hit chance.",
            new String[]{
                "Each point increases critical hit chance by 1%.",
                "Current crit chance: " + ChatColor.YELLOW + (ps.baseDexterity + ps.bonusDexterity) + "%."
            }
        ));
        inv.setItem(15, createStatBook(
            "Health", StatType.HP, ps.baseHealthStat, ps.bonusHealthStat, ps.skillPoints,
            "Increases your maximum health.",
            new String[]{
                "Each point increases max health by 2 HP.",
                "Current HP bonus: " + ChatColor.YELLOW + ((ps.baseHealthStat + ps.bonusHealthStat) * 2) + " HP."
            }
        ));

        // Updated Defense stat with diminishing returns
        int totalDef = ps.baseDefenceStat + ps.bonusDefenceStat;
        double percentReduction = totalDef / (totalDef + 100.0);
        percentReduction *= 100.0; // convert to percentage
        percentReduction = Math.round(percentReduction * 10.0) / 10.0; // round to 1 decimal

        inv.setItem(16, createStatBook(
            "Defense", StatType.DEF, ps.baseDefenceStat, ps.bonusDefenceStat, ps.skillPoints,
            "Reduces incoming damage.",
            new String[]{
                "Damage reduction scales with total Defense.",
                "Current damage reduction: " + ChatColor.YELLOW + percentReduction + "%"
            }
        ));

        // Refund Skill Points Button
        inv.setItem(22, createMenuItem(Material.ENDER_EYE,
            ChatColor.RED + "" + ChatColor.BOLD + "Refund All Skill Points",
            ChatColor.YELLOW + "Click twice to confirm."
        ));

        // Player head with overall summary
        inv.setItem(26, createPlayerHead(player, ps));

        // Fill empty slots with gray stained glass panes
        ItemStack filler = createFillerItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        return inv;
    }


    private static ItemStack createStatBook(
        String statName, StatType statType, int baseValue, int bonusValue, int skillPoints,
        String description, String[] effectDetails
    ) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        int displayValue = Math.min(baseValue + bonusValue, 64);
        book.setAmount(Math.max(displayValue, 1));

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Upgrade " + ChatColor.GREEN + statName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.WHITE + "Base: " + ChatColor.YELLOW + baseValue);
        lore.add(ChatColor.WHITE + "Bonus: " + ChatColor.GREEN + bonusValue);
        lore.add(ChatColor.WHITE + "Total: " + ChatColor.GOLD + (baseValue + bonusValue));
        for (String line : effectDetails) lore.add(ChatColor.WHITE + line);
        meta.setLore(lore);
        book.setItemMeta(meta);
        return book;
    }

    public static ItemStack createPlayerHead(Player player, PlayerStats ps) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + player.getName() + "'s Stats");

        List<String> lore = new ArrayList<>();
        LevelManager levelManager = LevelManager.getInstance();
        int currentXP = levelManager.getXP(player);
        int nextLevelXP = levelManager.getXpNeededForNextLevel(player);

        lore.add(ChatColor.GOLD + "Level: " + ChatColor.WHITE + StatsManager.getInstance().getLevel(player));
        lore.add("");
        lore.add(ChatColor.RED + "Health: " + ChatColor.YELLOW + (int) player.getHealth() + "/" + (int) player.getMaxHealth());
        lore.add(ChatColor.BLUE + "Mana: " + ChatColor.YELLOW + ps.currentMana + "/" + ps.maxMana);
        lore.add("");

        // Add all stats with gear bonuses
        lore.add(ChatColor.GRAY + "Strength: " + ChatColor.WHITE + (ps.baseStrength + ps.bonusStrength) + ChatColor.GREEN + " (+" + ps.bonusStrength + ")");
        lore.add(ChatColor.GRAY + "Agility: " + ChatColor.WHITE + (ps.baseAgility + ps.bonusAgility) + ChatColor.GREEN + " (+" + ps.bonusAgility + ")");
        lore.add(ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + (ps.baseIntelligence + ps.bonusIntelligence) + ChatColor.GREEN + " (+" + ps.bonusIntelligence + ")");
        lore.add(ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + (ps.baseDexterity + ps.bonusDexterity) + ChatColor.GREEN + " (+" + ps.bonusDexterity + ")");
        lore.add(ChatColor.GRAY + "Defense: " + ChatColor.WHITE + (ps.baseDefenceStat + ps.bonusDefenceStat) + ChatColor.GREEN + " (+" + ps.bonusDefenceStat + ")");
        lore.add("");

        lore.add(ChatColor.GOLD + "Total XP: " + ChatColor.WHITE + currentXP + ChatColor.GRAY + " / " + ChatColor.WHITE + nextLevelXP);
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack createMenuItem(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(loreText));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createFillerItem(Material material, String space) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(space);
        filler.setItemMeta(meta);
        return filler;
    }
}

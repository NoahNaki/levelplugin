package me.nakilex.levelplugin.player.attributes.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
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
import java.util.List;

public class StatsInventory {

    public static Inventory getStatsMenu(Player player) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(
            null,
            54,
            ps.skillPoints + " skill points remaining"
        );

        // Stat books with base and bonus stats
        inv.setItem(19, createStatBook(
            "Strength", StatType.STR, ps.baseStrength, ps.bonusStrength, ps.skillPoints,
            "Increases your melee damage.",
            new String[]{
                "Each point increases melee damage by 0.5.",
                "Current bonus: " + ChatColor.YELLOW + ((ps.baseStrength + ps.bonusStrength) * 0.5) + " damage."
            }
        ));

        // Agility with DR dodge chance
        int totalAgility = ps.baseAgility + ps.bonusAgility;
        double dodgePercent = totalAgility / (totalAgility + 200.0) * 100.0;
        dodgePercent = Math.round(dodgePercent * 10.0) / 10.0;  // 1-decimal precision

        inv.setItem(20, createStatBook(
            "Agility", StatType.AGI, ps.baseAgility, ps.bonusAgility, ps.skillPoints,
            "Improves your speed and dodge chance.",
            new String[]{
                "Dodge chance scales with total Agility.",
                "Current dodge chance: " + ChatColor.YELLOW + dodgePercent + "%.",
                "Speed bonus: +" + ((ps.baseAgility + ps.bonusAgility) * 0.001f)
            }
        ));

        inv.setItem(21, createStatBook(
            "Intelligence", StatType.INT, ps.baseIntelligence, ps.bonusIntelligence, ps.skillPoints,
            "Increases your max mana and mana regeneration.",
            new String[]{
                "Each point adds 10 max mana and 0.05 mana/sec.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana,
                "Mana regen: +" + ((ps.baseIntelligence + ps.bonusIntelligence) * 0.05) + " mana/sec."
            }
        ));

        int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
        double critPercent  = totalDexterity / (totalDexterity + 100.0) * 100.0;
        critPercent = Math.round(critPercent * 10.0) / 10.0;
        inv.setItem(23, createStatBook(
            "Dexterity", StatType.DEX, ps.baseDexterity, ps.bonusDexterity, ps.skillPoints,
            "Improves crit chance and subtracts from enemy dodge based on your DEX.",
            new String[]{
                "Crit chance: " + ChatColor.YELLOW + critPercent + "% (DR formula).",
                "Accuracy: subtracts " + totalDexterity + " Agility points from the target before dodge."
            }
        ));

        inv.setItem(24, createStatBook(
            "Vitality", StatType.HP, ps.baseHealthStat, ps.bonusHealthStat, ps.skillPoints,
            "Increases your maximum health and stamina.",
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

        inv.setItem(25, createStatBook(
            "Defense", StatType.DEF, ps.baseDefenceStat, ps.bonusDefenceStat, ps.skillPoints,
            "Reduces incoming damage.",
            new String[]{
                "Damage reduction scales with total Defense.",
                "Current damage reduction: " + ChatColor.YELLOW + percentReduction + "%"
            }
        ));

        // Refund Skill Points Button
        inv.setItem(13, getNexoItem("refresh", ChatColor.RED + "Refund All Skill Points"));

        // Player head with overall summary
        inv.setItem(8, createPlayerHead(player, ps));

        // Navigation and extra buttons
        inv.setItem(37, getNexoItem("arrow_left", ChatColor.GRAY + "Equip Runes"));
        inv.setItem(43, getNexoItem("arrow_right", ChatColor.GRAY + "Equip Runes"));
        inv.setItem(48, getNexoItem("camera", ChatColor.YELLOW + "Coming Soon"));
        inv.setItem(50, getNexoItem("settings", ChatColor.AQUA + "Settings"));

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

    private static ItemStack createFillerItem(Material material, String space) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(space);
        filler.setItemMeta(meta);
        return filler;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}

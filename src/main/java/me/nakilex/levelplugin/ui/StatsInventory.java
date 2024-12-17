package me.nakilex.levelplugin.ui;

import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.managers.StatsManager.StatType;
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
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Stats");

        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);

        // Stat books
        inv.setItem(10, createStatBook(
            "Strength", StatType.STR, ps.strength, ps.skillPoints,
            "Increases your melee damage.",
            new String[]{
                "Each point increases melee damage by 0.5.",
                "Current bonus: " + ChatColor.YELLOW + (ps.strength * 0.5) + " damage."
            }
        ));
        inv.setItem(11, createStatBook(
            "Agility", StatType.AGI, ps.agility, ps.skillPoints,
            "Improves your speed and dodge chance.",
            new String[]{
                "Each point increases dodge chance by 1% and speed by 0.001.",
                "Current dodge chance: " + ChatColor.YELLOW + ps.agility + "%.",
                "Speed bonus: +" + (ps.agility * 0.001f)
            }
        ));
        inv.setItem(12, createStatBook(
            "Intelligence", StatType.INT, ps.intelligence, ps.skillPoints,
            "Increases your max mana and mana regeneration.",
            new String[]{
                "Each point adds 10 max mana and 0.05 mana/sec.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana,
                "Mana regen: +" + (ps.intelligence * 0.05) + " mana/sec."
            }
        ));
        inv.setItem(14, createStatBook(
            "Dexterity", StatType.DEX, ps.dexterity, ps.skillPoints,
            "Improves your critical hit chance.",
            new String[]{
                "Each point increases critical hit chance by 1%.",
                "Current crit chance: " + ChatColor.YELLOW + ps.dexterity + "%."
            }
        ));
        inv.setItem(15, createStatBook(
            "Health", StatType.HP, ps.healthStat, ps.skillPoints,
            "Increases your maximum health.",
            new String[]{
                "Each point increases max health by 2 HP.",
                "Current HP bonus: " + ChatColor.YELLOW + (ps.healthStat * 2) + " HP."
            }
        ));
        inv.setItem(16, createStatBook(
            "Defense", StatType.DEF, ps.defenceStat, ps.skillPoints,
            "Reduces incoming damage.",
            new String[]{
                "Each point reduces damage taken by 0.5.",
                "Current damage reduction: " + ChatColor.YELLOW + (ps.defenceStat * 0.5) + " damage."
            }
        ));

        // Refund Skill Points Button
        inv.setItem(22, createMenuItem(Material.ENDER_EYE,
            ChatColor.RED + "" + ChatColor.BOLD + "Refund All Skill Points",
            ChatColor.YELLOW + "Click twice to confirm."
        ));

        // Player head with overall summary
        inv.setItem(26, createPlayerHead(player, ps));

        // Fill all empty slots with gray stained glass panes
        ItemStack filler = createFillerItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        return inv;
    }

    private static ItemStack createFillerItem(Material material, String space) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" "); // Blank display name
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide attributes
            filler.setItemMeta(meta);
        }
        return filler;
    }

    private static ItemStack createStatBook(
        String statName,
        StatType statType,
        int currentValue,
        int skillPoints,
        String description,
        String[] effectDetails
    ) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        // If value exceeds the max stack size, cap it at 64 for visual representation
        int displayValue = Math.min(currentValue, 64);
        book.setAmount(Math.max(displayValue, 1)); // Ensure at least 1 is displayed

        // Display title
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Upgrade your " + ChatColor.GREEN + statName + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + " skill");
        // Create the lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + description);
        lore.add("");

        // Always show the actual stat value in the lore
        lore.add(ChatColor.WHITE + "Current " + statName + " Value: " + ChatColor.YELLOW + currentValue);

        // Add any custom effect details
        for (String line : effectDetails) {
            lore.add(ChatColor.WHITE + line);
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "Left-Click to add 1 point");
        lore.add(ChatColor.RED + "Right-Click to remove 1 point");
        lore.add(ChatColor.YELLOW + "Hold Shift to modify by 5");


        meta.setLore(lore);
        book.setItemMeta(meta);

        return book;
    }





    private static ItemStack createPlayerHead(Player player, PlayerStats ps) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + player.getName() + "'s Info");

        List<String> lore = new ArrayList<>();

        // Add level to the lore
        int level = StatsManager.getInstance().getLevel(player);
        lore.add(ChatColor.GOLD + "Level: " + ChatColor.WHITE + level);
        lore.add("");
        lore.add(ChatColor.RED + "HP: " + ChatColor.BOLD + ChatColor.RED + "♥ " + ChatColor.YELLOW + (int) player.getHealth() + "/" + (int) player.getMaxHealth());
        lore.add(ChatColor.BLUE + "Mana: " + ChatColor.BOLD + ChatColor.BLUE + "✦ " + ChatColor.YELLOW + (int) ps.currentMana + "/" + ps.maxMana);
        lore.add("");
        lore.add(ChatColor.GRAY + "STR: " + ChatColor.WHITE + ps.strength);
        lore.add(ChatColor.GRAY + "AGI: " + ChatColor.WHITE + ps.agility);
        lore.add(ChatColor.GRAY + "INT: " + ChatColor.WHITE + ps.intelligence);
        lore.add(ChatColor.GRAY + "DEX: " + ChatColor.WHITE + ps.dexterity);
        lore.add(ChatColor.GRAY + "DEF: " + ChatColor.WHITE + ps.defenceStat);
        lore.add("");
        String currentClass = ps.playerClass != null ? ps.playerClass.name() : "None";
        lore.add(ChatColor.GRAY + "Class: " + ChatColor.WHITE + currentClass);

        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);

        return head;
    }

    private static ItemStack createMenuItem(Material mat, String displayName, String s) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Collections.emptyList()); // Clear default lore
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide item attributes like attack damage
            item.setItemMeta(meta);
        }
        return item;
    }
}

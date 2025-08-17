package me.nakilex.levelplugin.player.attributes.gui;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
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

    private static final int MAX_PAGES = 2;
    private static final java.util.Map<java.util.UUID, Integer> pageMap = new java.util.HashMap<>();

    public static int getPage(Player player) {
        return pageMap.getOrDefault(player.getUniqueId(), 0);
    }

    public static void setPage(Player player, int page) {
        pageMap.put(player.getUniqueId(), page % MAX_PAGES);
    }

    public static Inventory getStatsMenu(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        return getStatsMenu(player, page);
    }

    public static Inventory getStatsMenu(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(
            null,
            54,
            ps.skillPoints + " skill points remaining"
        );
        inv.setItem(19, createStatBook(
            "Strength", StatType.STR, ps.baseStrength, ps.bonusStrength, ps.skillPoints,
            "Increases your melee damage.",
            new String[]{
                "Each point increases melee damage by 0.5.",
                "Current bonus: " + ChatColor.YELLOW + ((ps.baseStrength + ps.bonusStrength) * 0.5) + " damage."
            }
        ));
        int totalAgility = ps.baseAgility + ps.bonusAgility;
        double dodgePercent = totalAgility / (totalAgility + 200.0) * 100.0;
        dodgePercent = Math.round(dodgePercent * 10.0) / 10.0;

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
            "Vitality", StatType.VIT, ps.baseVitality, ps.bonusVitality, ps.skillPoints,
            "Increases max health and reduces damage taken.",
            new String[]{
                "Each point grants 3 HP.",
                "Current HP bonus: " + ChatColor.YELLOW + ((ps.baseVitality + ps.bonusVitality) * 3) + " HP."
            }
        ));

        inv.setItem(25, createStatBook(
            "Will", StatType.WIL, ps.baseWill, ps.bonusWill, ps.skillPoints,
            "Boosts mana and mana regeneration.",
            new String[]{
                "Each point adds 3 max mana and 0.25 mana/sec.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana
            }
        ));
        inv.setItem(13, GuiUtil.getNexoItem("refresh", ChatColor.RED + "Refund All Skill Points"));
        inv.setItem(8, createPlayerHead(player, ps, page));
        inv.setItem(37, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Back"));
        inv.setItem(43, GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Forward"));
        inv.setItem(48, GuiUtil.getNexoItem("camera", ChatColor.YELLOW + "Coming Soon"));
        inv.setItem(50, GuiUtil.getNexoItem("settings", ChatColor.AQUA + "Settings"));
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
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

    public static ItemStack createPlayerHead(Player player, PlayerStats ps, int page) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        String title = page == 0 ? "Character Info" : "Profession Info";
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + title);

        List<String> lore = new ArrayList<>();
        LevelManager levelManager = LevelManager.getInstance();
        me.nakilex.levelplugin.player.mining.managers.MiningManager miningManager = me.nakilex.levelplugin.player.mining.managers.MiningManager.getInstance();
        int currentXP = levelManager.getXP(player);
        int nextLevelXP = levelManager.getXpNeededForNextLevel(player);
        if (page == 0) {
            lore.add("");
            lore.add(ChatColor.BLUE + "\u2620 " + ChatColor.GRAY + "Strength: " + ChatColor.WHITE + (ps.baseStrength + ps.bonusStrength) + ChatColor.GREEN + " (+" + ps.bonusStrength + ")");
            lore.add(ChatColor.GREEN + "\u2248 " + ChatColor.GRAY + "Agility: " + ChatColor.WHITE + (ps.baseAgility + ps.bonusAgility) + ChatColor.GREEN + " (+" + ps.bonusAgility + ")");
            lore.add(ChatColor.AQUA + "\u2666 " + ChatColor.GRAY + "Intelligence: " + ChatColor.WHITE + (ps.baseIntelligence + ps.bonusIntelligence) + ChatColor.GREEN + " (+" + ps.bonusIntelligence + ")");
            lore.add(ChatColor.YELLOW + "\u27B9 " + ChatColor.GRAY + "Dexterity: " + ChatColor.WHITE + (ps.baseDexterity + ps.bonusDexterity) + ChatColor.GREEN + " (+" + ps.bonusDexterity + ")");
            lore.add(ChatColor.RED + "\u2764 " + ChatColor.GRAY + "Vitality: " + ChatColor.WHITE + (ps.baseVitality + ps.bonusVitality) + ChatColor.GREEN + " (+" + ps.bonusVitality + ")");
            lore.add(ChatColor.BLUE + "\u272A " + ChatColor.GRAY + "Will: " + ChatColor.WHITE + (ps.baseWill + ps.bonusWill) + ChatColor.GREEN + " (+" + ps.bonusWill + ")");
            lore.add("");

            int gearScore = ItemUtil.calculateTotalGearScore(player);
            lore.add("<glyph:sword_icon> " + ChatColor.GRAY + "Gear Score: "
                    + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
            lore.add(""); // divider after Gear Score

            double progress = nextLevelXP > 0 ? (double) currentXP / nextLevelXP : 0.0;
            double percent = Math.round(progress * 1000.0) / 10.0;
            lore.add(ChatColor.GRAY + "Progress to Level " + ChatColor.YELLOW + (StatsManager.getInstance().getLevel(player) + 1) + ChatColor.GRAY + ": " + ChatColor.YELLOW + percent + "%");
            String bar = GuiUtil.createProgressBar(progress, 15);
            String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
            lore.add(bar + " " + ChatColor.YELLOW + currentXP + ChatColor.GOLD + "/" + ChatColor.YELLOW + nextLevelXP + " <glyph:experience_orb_icon> " + expLabel);
        } else {
            int mLevel = miningManager.getLevel(player);
            int next = miningManager.getXpRequired(mLevel);
            int mXp = miningManager.getXP(player);
            double percent = next > 0 ? (mXp * 100.0 / next) : 0.0;
            percent = Math.round(percent * 10.0) / 10.0;

            lore.add(ChatColor.GRAY + "General information about");
            lore.add(ChatColor.GRAY + "your characters profressions");
            lore.add("");
            lore.add(ChatColor.GOLD + "Gathering Skills:");
            lore.add(ChatColor.GOLD + "- " + ChatColor.GRAY + "Lv. " + mLevel + " Mining "
                + ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GRAY + percent + "%]");
        }

        lore.add(" ");
        String box1 = (page == 0 ? ChatColor.GREEN : ChatColor.DARK_GRAY) + "■";
        String box2 = (page == 1 ? ChatColor.GREEN : ChatColor.DARK_GRAY) + "■";
        lore.add(ChatColor.GREEN + "< " + box1 + " " + box2 + ChatColor.GREEN + " >");
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

}

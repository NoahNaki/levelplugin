package me.nakilex.levelplugin.player.attributes.gui;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

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
        GuiBuilder builder = GuiBuilder.create(54, ps.skillPoints + " skill points remaining")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        builder.setItem(19, createStatBook(
            "Strength", StatType.STR, ps.baseStrength, ps.bonusStrength, ps.skillPoints,
            "Boosts melee damage and adds a bit of health.",
            new String[]{
                "Each point: +0.5 melee dmg & +1 HP.",
                "HP bonus: " + ChatColor.YELLOW + ((ps.baseStrength + ps.bonusStrength) * 1) + " HP"
            }
        ));
        int totalAgility = ps.baseAgility + ps.bonusAgility;
        double dodgePercent = totalAgility / (totalAgility + 200.0) * 100.0;
        dodgePercent = Math.round(dodgePercent * 10.0) / 10.0;

        builder.setItem(20, createStatBook(
            "Agility", StatType.AGI, ps.baseAgility, ps.bonusAgility, ps.skillPoints,
            "Improves your speed and dodge chance.",
            new String[]{
                "Dodge chance scales with total Agility.",
                "Current dodge chance: " + ChatColor.YELLOW + dodgePercent + "%.",
                "Speed bonus: +" + ((ps.baseAgility + ps.bonusAgility) * 0.001f)
            }
        ));

        builder.setItem(21, createStatBook(
            "Intelligence", StatType.INT, ps.baseIntelligence, ps.bonusIntelligence, ps.skillPoints,
            "Improves magical prowess and max mana.",
            new String[]{
                "Each point: +0.5 magic dmg & +1 Mana.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana
            }
        ));

        int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
        double critPercent  = totalDexterity / (totalDexterity + 100.0) * 100.0;
        critPercent = Math.round(critPercent * 10.0) / 10.0;
        builder.setItem(23, createStatBook(
            "Dexterity", StatType.DEX, ps.baseDexterity, ps.bonusDexterity, ps.skillPoints,
            "Improves crit chance and subtracts from enemy dodge based on your DEX.",
            new String[]{
                "Crit chance: " + ChatColor.YELLOW + critPercent + "% (DR formula).",
                "Accuracy: subtracts " + totalDexterity + " Agility points from the target before dodge."
            }
        ));

        builder.setItem(24, createStatBook(
            "Vitality", StatType.VIT, ps.baseVitality, ps.bonusVitality, ps.skillPoints,
            "Increases max health and reduces damage taken.",
            new String[]{
                "Each point grants 3 HP and defense.",
                "Current HP bonus: " + ChatColor.YELLOW + ((ps.baseVitality + ps.bonusVitality) * 3) + " HP."
            }
        ));

        builder.setItem(25, createStatBook(
            "Will", StatType.WIL, ps.baseWill, ps.bonusWill, ps.skillPoints,
            "Boosts mana and mana regeneration.",
            new String[]{
                "Each point: +3 max mana & +0.25 mana/sec.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana
            }
        ));

        double atkSpeed = 0.5 * (1.0 + 0.01 * (ps.baseTechnique + ps.bonusTechnique));
        builder.setItem(22, createStatBook(
            "Technique", StatType.TEC, ps.baseTechnique, ps.bonusTechnique, ps.skillPoints,
            "Amplifies attack speed and all damage.",
            new String[]{
                "+1% atk speed & +0.3 dmg per point.",
                "Current atk speed: " + ChatColor.YELLOW + atkSpeed + " attacks/s"
            }
        ));
        builder.setItem(13, GuiUtil.getNexoItem("refresh", ChatColor.RED + "Refund All Skill Points"));
        builder.setItem(8, createPlayerHead(player, ps, page));
        builder.setItem(37, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Back"));
        builder.setItem(43, GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Forward"));
        builder.setItem(48, GuiUtil.getNexoItem("camera", ChatColor.YELLOW + "Coming Soon"));
        builder.setItem(50, GuiUtil.getNexoItem("settings", ChatColor.AQUA + "Settings"));

        return builder.build();
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
        meta.setLore(TooltipUtil.centerLore(lore));
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
            lore.add(ChatColor.DARK_PURPLE + "\u2694 " + ChatColor.GRAY + "Technique: " + ChatColor.WHITE + (ps.baseTechnique + ps.bonusTechnique) + ChatColor.GREEN + " (+" + ps.bonusTechnique + ")");
            lore.add("");

            int gearScore = ItemUtil.calculateTotalGearScore(player);
            lore.add("<glyph:sword_icon> " + ChatColor.GRAY + "Gear Score: "
                    + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
            lore.add(""); // divider after Gear Score

            double progress = nextLevelXP > 0 ? (double) currentXP / nextLevelXP : 0.0;
            double percent = Math.round(progress * 1000.0) / 10.0;
            lore.add(ChatColor.GRAY + "Progress to Level " + ChatColor.YELLOW + (StatsManager.getInstance().getLevel(player) + 1) + ChatColor.GRAY + ": " + ChatColor.YELLOW + percent + "%");
            String bar = TooltipUtil.progressBar(currentXP, nextLevelXP, 15);
            String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
            String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
            lore.add(bar + " " + expColor + currentXP + ChatColor.GOLD + "/" + expColor + nextLevelXP + " <glyph:experience_orb_icon> " + expLabel);
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
        meta.setLore(TooltipUtil.centerLore(lore));
        head.setItemMeta(meta);
        return head;
    }

}

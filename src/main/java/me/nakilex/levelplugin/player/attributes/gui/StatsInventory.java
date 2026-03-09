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
            List.of("Boosts melee damage and adds a bit of health."),
            new String[]{
                String.format("Each point: +%.2f melee dmg & +%.2f HP.", 0.5, StatsManager.HEALTH_PER_STRENGTH),
                String.format("HP bonus: %s%.2f HP", ChatColor.YELLOW,
                        (ps.baseStrength + ps.bonusStrength) * StatsManager.HEALTH_PER_STRENGTH)
            }
        ));
        int totalAgility = ps.baseAgility + ps.bonusAgility;
        double dodgePercent = totalAgility / (totalAgility + 200.0) * 100.0;
        dodgePercent = Math.round(dodgePercent * 100.0) / 100.0;

        builder.setItem(20, createStatBook(
            "Agility", StatType.AGI, ps.baseAgility, ps.bonusAgility, ps.skillPoints,
            List.of("Improves your speed and dodge chance."),
            new String[]{
                "Dodge chance scales with total Agility.",
                "A successful dodge reduces damage by 90%.",
                "Current dodge chance: " + ChatColor.YELLOW + String.format("%.2f", dodgePercent) + "%.",
                "Speed bonus: +" + String.format("%.2f", (ps.baseAgility + ps.bonusAgility) * 0.001f)
            }
        ));

        builder.setItem(21, createStatBook(
            "Intelligence", StatType.INT, ps.baseIntelligence, ps.bonusIntelligence, ps.skillPoints,
            List.of("Improves magical prowess and max mana."),
            new String[]{
                "Each point: +0.5 magic dmg & +1 Mana.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana
            }
        ));

        int totalDexterity = ps.baseDexterity + ps.bonusDexterity;
        double critPercent  = totalDexterity / (totalDexterity + 100.0) * 100.0;
        critPercent = Math.round(critPercent * 100.0) / 100.0;
        builder.setItem(23, createStatBook(
            "Dexterity", StatType.DEX, ps.baseDexterity, ps.bonusDexterity, ps.skillPoints,
            List.of(
                "Improves crit chance and subtracts",
                "from enemy dodge based on your DEX."
            ),
            new String[]{
                "Crit chance: " + ChatColor.YELLOW + String.format("%.2f", critPercent) + "% (DR formula).",
                "Accuracy: subtracts " + totalDexterity + " Agility points",
                "from the target before dodge."
            }
        ));

        builder.setItem(24, createStatBook(
            "Vitality", StatType.VIT, ps.baseVitality, ps.bonusVitality, ps.skillPoints,
            List.of("Increases max health and reduces damage taken."),
            new String[]{
                String.format("Each point grants %.2f HP and defense.", StatsManager.HEALTH_PER_VITALITY),
                String.format("Current HP bonus: %s%.2f HP.", ChatColor.YELLOW,
                        (ps.baseVitality + ps.bonusVitality) * StatsManager.HEALTH_PER_VITALITY)
            }
        ));

        builder.setItem(25, createStatBook(
            "Will", StatType.WIL, ps.baseWill, ps.bonusWill, ps.skillPoints,
            List.of("Boosts mana and mana regeneration."),
            new String[]{
                "Each point: +3 max mana & +0.25 mana/sec.",
                "Current max mana: " + ChatColor.YELLOW + ps.maxMana
            }
        ));

        double atkSpeed = 0.5 * (1.0 + 0.0075 * (ps.baseTechnique + ps.bonusTechnique));
        builder.setItem(22, createStatBook(
            "Technique", StatType.TEC, ps.baseTechnique, ps.bonusTechnique, ps.skillPoints,
            List.of("Amplifies attack speed and all damage."),
            new String[]{
                "+0.75% atk speed & +0.1 dmg per point.",
                "Current atk speed: " + ChatColor.YELLOW + String.format("%.2f", atkSpeed) + " attacks/s"
            }
        ));
        builder.setItem(13, GuiUtil.getNexoItem("refresh", ChatColor.RED + "Refund All Skill Points"));
        builder.setItem(8, createPlayerHead(player, ps, page));
        builder.setItem(37, GuiUtil.getNexoItem("arrow_left", ChatColor.GRAY + "Back"));
        builder.setItem(43, GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Forward"));
        builder.setItem(49, createLifeSkillButton());
        builder.setItem(40, createEssenceButton());

        return builder.build();
    }

    public static ItemStack createLifeSkillButton() {
        ItemStack lifeSkills = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta meta = lifeSkills.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Life Skills");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Review your profession progress.");
            lore.addAll(TooltipUtil.bulletList(
                    "Check mining and farming levels.",
                    "See progress toward the next tier."
            ));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to open the life skill log", null));
            meta.setLore(lore);
            lifeSkills.setItemMeta(meta);
        }
        return lifeSkills;
    }

    private static ItemStack createEssenceButton() {
        ItemStack essences = GuiUtil.getNexoItem("essence_icon", ChatColor.LIGHT_PURPLE + "Essences");
        ItemMeta meta = essences.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "View and swap your class essences.");
            lore.addAll(TooltipUtil.bulletList(
                    "Equip or unequip class essences.",
                    "Invest duplicates for quick upgrades."
            ));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to open the essence menu", null));
            meta.setLore(lore);
            essences.setItemMeta(meta);
        }
        return essences;
    }


    private static ItemStack createStatBook(
        String statName, StatType statType, int baseValue, int bonusValue, int skillPoints,
        List<String> description, String[] effectDetails
    ) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        int displayValue = Math.min(baseValue + bonusValue, 64);
        book.setAmount(Math.max(displayValue, 1));

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Upgrade " + ChatColor.GREEN + statName);
        List<String> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(ChatColor.GRAY + line);
        }
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Stat Breakdown");
        lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Base: " + ChatColor.WHITE + baseValue));
        lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Bonus: " + ChatColor.GREEN + "+" + bonusValue));
        lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Total: " + ChatColor.YELLOW + (baseValue + bonusValue)));
        lore.add("");
        lore.add(ChatColor.GOLD + "Perks");
        for (String line : effectDetails) {
            lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + line));
        }
        lore.add("");
        lore.addAll(TooltipUtil.clickInstructions("to invest a point", "to remove a point"));
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
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.STR) + ": " + ChatColor.WHITE + (ps.baseStrength + ps.bonusStrength) + ChatColor.GREEN + " (+" + ps.bonusStrength + ")");
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.AGI) + ": " + ChatColor.WHITE + (ps.baseAgility + ps.bonusAgility) + ChatColor.GREEN + " (+" + ps.bonusAgility + ")");
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.INT) + ": " + ChatColor.WHITE + (ps.baseIntelligence + ps.bonusIntelligence) + ChatColor.GREEN + " (+" + ps.bonusIntelligence + ")");
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.DEX) + ": " + ChatColor.WHITE + (ps.baseDexterity + ps.bonusDexterity) + ChatColor.GREEN + " (+" + ps.bonusDexterity + ")");
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.VIT) + ": " + ChatColor.RED + (ps.baseVitality + ps.bonusVitality) + ChatColor.GREEN + " (+" + ps.bonusVitality + ")");
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.WIL) + ": " + ChatColor.WHITE + (ps.baseWill + ps.bonusWill) + ChatColor.GREEN + " (+" + ps.bonusWill + ")");
            lore.add(GuiUtil.formatStatName(StatsManager.StatType.TEC) + ": " + ChatColor.WHITE + (ps.baseTechnique + ps.bonusTechnique) + ChatColor.GREEN + " (+" + ps.bonusTechnique + ")");
            lore.add("");

            int gearScore = ItemUtil.calculateTotalGearScore(player);
              lore.add(ChatColor.GRAY + "Gear Score: "
                      + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gearScore);
            lore.add(""); // divider after Gear Score

            double progress = nextLevelXP > 0 ? (double) currentXP / nextLevelXP : 0.0;
            double percent = Math.round(progress * 10000.0) / 100.0;
            lore.add(ChatColor.GRAY + "Progress to Level " + ChatColor.YELLOW + (StatsManager.getInstance().getLevel(player) + 1) + ChatColor.GRAY + ": " + ChatColor.YELLOW + String.format("%.2f", percent) + "%");
            String bar = TooltipUtil.expProgressBarByPixels(currentXP, nextLevelXP, 140);
            String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
            String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
            lore.add(bar + " " + expColor + currentXP + ChatColor.GOLD + "/" + expColor + nextLevelXP + " <glyph:experience_orb_icon> " + expLabel);
        } else {
            int mLevel = miningManager.getLevel(player);
            int next = miningManager.getXpRequired(mLevel);
            int mXp = miningManager.getXP(player);
            double percent = next > 0 ? (mXp * 100.0 / next) : 0.0;
            percent = Math.round(percent * 100.0) / 100.0;

            me.nakilex.levelplugin.player.farming.managers.FarmingManager farmingManager =
                    me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance();
            int fLevel = farmingManager.getLevel(player);
            int fNext = farmingManager.getXpRequired(fLevel);
            int fXp = farmingManager.getXP(player);
            double fPercent = fNext > 0 ? (fXp * 100.0 / fNext) : 0.0;
            fPercent = Math.round(fPercent * 100.0) / 100.0;

            me.nakilex.levelplugin.player.fishing.managers.FishingManager fishingManager =
                    me.nakilex.levelplugin.player.fishing.managers.FishingManager.getInstance();
            int fiLevel = fishingManager.getLevel(player);
            int fiNext = fishingManager.getXpRequired(fiLevel);
            int fiXp = fishingManager.getXP(player);
            double fiPercent = fiNext > 0 ? (fiXp * 100.0 / fiNext) : 0.0;
            fiPercent = Math.round(fiPercent * 100.0) / 100.0;

            lore.add(ChatColor.GRAY + "General information about");
            lore.add(ChatColor.GRAY + "your characters profressions");
            lore.add("");
            lore.add(ChatColor.GOLD + "Gathering Skills:");
            lore.add(ChatColor.GOLD + "- " + ChatColor.GRAY + "Lv. " + mLevel + " Mining "
                + ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GRAY + String.format("%.2f", percent) + "%]");
            lore.add(ChatColor.GOLD + "- " + ChatColor.GRAY + "Lv. " + fLevel + " Farming "
                + ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GRAY + String.format("%.2f", fPercent) + "%]");
            lore.add(ChatColor.GOLD + "- " + ChatColor.GRAY + "Lv. " + fiLevel + " Fishing "
                + ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GRAY + String.format("%.2f", fiPercent) + "%]");
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

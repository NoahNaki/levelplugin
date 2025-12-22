package me.nakilex.levelplugin.player.attributes.gui;

import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager;
import me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager.LifeSkillReward;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class LifeSkillRewardsGUI {

    private static final int[] PATH = {
            10,11,12,13,14,15,16,
            25,24,23,22,21,20,19,
            28,29,30,31,32,33,34,
            43,42,41,40,39,38,37
    };

    private LifeSkillRewardsGUI() {}

    public static String titleFor(ToolDiscipline discipline) {
        return ChatColor.GOLD + "" + ChatColor.BOLD + discipline.name().substring(0,1).toUpperCase()
                + discipline.name().substring(1).toLowerCase() + " Skill Rewards";
    }

    public static ToolDiscipline disciplineFromTitle(String title) {
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            if (titleFor(discipline).equals(title)) {
                return discipline;
            }
        }
        return null;
    }

    public static void open(Player player, ToolDiscipline discipline) {
        player.openInventory(create(player, discipline));
    }

    public static Inventory create(Player player, ToolDiscipline discipline) {
        GuiBuilder builder = GuiBuilder.create(54, titleFor(discipline))
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        LifeSkillRewardManager rewardManager = LifeSkillRewardManager.getInstance();
        List<LifeSkillReward> rewards = rewardManager.getRewards(discipline);

        int level = discipline == ToolDiscipline.MINING
                ? MiningManager.getInstance().getLevel(player)
                : FarmingManager.getInstance().getLevel(player);

        for (int i = 0; i < PATH.length; i++) {
            ItemStack tile;
            if (i < rewards.size()) {
                LifeSkillReward reward = rewards.get(i);
                tile = createRewardItem(player, discipline, reward, level,
                        rewardManager.isClaimed(player.getUniqueId(), discipline, reward.levelRequired()));
            } else {
                tile = createPlaceholder();
            }
            builder.setItem(PATH[i], tile);
        }

        builder.setItem(45, createBackButton());

        return builder.build();
    }

    public static int indexForSlot(int slot) {
        for (int i = 0; i < PATH.length; i++) {
            if (PATH[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack createRewardItem(Player player, ToolDiscipline discipline, LifeSkillReward reward,
                                              int playerLevel, boolean claimed) {
        boolean available = playerLevel >= reward.levelRequired();
        Material material;
        if (claimed) {
            material = Material.YELLOW_STAINED_GLASS_PANE;
        } else if (available) {
            material = Material.LIME_STAINED_GLASS_PANE;
        } else {
            material = Material.RED_STAINED_GLASS_PANE;
        }

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String skillName = discipline.name().substring(0, 1).toUpperCase() + discipline.name().substring(1).toLowerCase();
            meta.setDisplayName(reward.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Requirement: " + ChatColor.YELLOW + skillName + " "
                    + reward.levelRequired());
            lore.add("");
            lore.addAll(reward.lore());
            lore.add("");
            if (claimed) {
                lore.add(ChatColor.GREEN + "Already claimed.");
            } else if (available) {
                lore.add(ChatColor.GOLD + "Click to claim your reward!");
                lore.addAll(TooltipUtil.clickInstructions("to collect", null));
            } else {
                lore.add(ChatColor.RED + "Reach the required level to claim." );
            }
            lore.add("");
            double progress = Math.min(1.0, playerLevel / (double) Math.max(1, reward.levelRequired()));
            lore.add(ChatColor.GRAY + "Progress:");
            lore.add(ChatColor.YELLOW + TooltipUtil.progressBar(playerLevel, reward.levelRequired(), 20));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack createPlaceholder() {
        ItemStack stack = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Future Milestone");
            meta.setLore(List.of(ChatColor.GRAY + "Progress further to unlock more rewards."));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack createBackButton() {
        ItemStack back = GuiUtil.getNexoItem("arrow_left", ChatColor.RED + "Back to Skills");
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Return to the life skill list.");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to go back", null));
            meta.setLore(lore);
            back.setItemMeta(meta);
        }
        return back;
    }
}

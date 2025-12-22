package me.nakilex.levelplugin.player.attributes.gui;

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

public final class LifeSkillGUI {

    public static final String TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Life Skills";

    private LifeSkillGUI() {}

    public static void open(Player player) {
        player.openInventory(create(player));
    }

    public static Inventory create(Player player) {
        GuiBuilder builder = GuiBuilder.create(45, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        MiningManager miningManager = MiningManager.getInstance();
        FarmingManager farmingManager = FarmingManager.getInstance();

        builder.setItem(20, createSkillItem(
                "Mining",
                Material.DIAMOND_PICKAXE,
                miningManager.getLevel(player),
                miningManager.getXP(player),
                miningManager.getXpRequired(miningManager.getLevel(player)),
                miningManager.getMaxLevel(),
                TooltipUtil.bulletList(
                        "Improve ore yields and access higher tier nodes.",
                        "Tool bonuses scale with your mining level."
                )
        ));

        builder.setItem(24, createSkillItem(
                "Farming",
                Material.GOLDEN_HOE,
                farmingManager.getLevel(player),
                farmingManager.getXP(player),
                farmingManager.getXpRequired(farmingManager.getLevel(player)),
                farmingManager.getMaxLevel(),
                TooltipUtil.bulletList(
                        "Harvest mature crops for the best XP and wheat.",
                        "Higher farming levels improve harvest rewards."
                )
        ));

        builder.setItem(40, createBackButton());

        return builder.build();
    }

    private static ItemStack createSkillItem(String name, Material icon, int level, int xp, int required, int maxLevel,
                                             List<String> extras) {
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            boolean capped = level >= maxLevel;
            int displayRequired = capped ? 1 : Math.max(required, 1);
            double progress = capped ? 1.0 : Math.min(1.0, xp / (double) displayRequired);
            double percent = Math.round(progress * 10000.0) / 100.0;

            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + name
                    + ChatColor.GRAY + " (Lv. " + ChatColor.WHITE + level + ChatColor.GRAY + ")");

            List<String> lore = new ArrayList<>();
            if (capped) {
                lore.add(ChatColor.GRAY + "You've reached the cap for this life skill.");
            } else {
                lore.add(ChatColor.GRAY + "Progress toward the next level:");
                lore.add(ChatColor.GOLD + TooltipUtil.progressBar(xp, displayRequired, 20));
                String progressLine = ChatColor.WHITE + "" + xp
                        + ChatColor.GRAY + "/" + ChatColor.WHITE + displayRequired
                        + ChatColor.GRAY + " (" + ChatColor.YELLOW + String.format("%.2f", percent) + "%" + ChatColor.GRAY + ")";
                lore.add(progressLine);
            }

            if (extras != null && !extras.isEmpty()) {
                lore.add("");
                lore.addAll(extras);
            }

            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to view rewards", null));

            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack createBackButton() {
        ItemStack back = GuiUtil.getNexoItem("arrow_left", ChatColor.RED + "Back to Stats");
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Return to your attribute overview.");
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to go back", null));
            meta.setLore(lore);
            back.setItemMeta(meta);
        }
        return back;
    }
}

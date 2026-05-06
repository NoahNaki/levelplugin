package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Reusable menu renderer for a single stage-based dungeon definition. */
public class StagedDungeonGUI implements Listener {
    private static final int SIZE = 27;

    private final StagedDungeonManager manager;
    private final StagedDungeonDefinition definition;
    private final String title;

    public StagedDungeonGUI(StagedDungeonManager manager, StagedDungeonDefinition definition) {
        this.manager = manager;
        this.definition = definition;
        this.title = definition.displayName();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, title);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(org.bukkit.Material.GRAY_STAINED_GLASS_PANE));
        renderEntryButton(inv, player);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GuiUtil.titleMatches(event.getView().getTitle(), title)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getRawSlot() != 13) return;
        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            manager.sweep(player, definition);
            renderEntryButton(event.getView().getTopInventory(), player);
            return;
        }
        if (event.getClick().isLeftClick()) {
            player.closeInventory();
            manager.startStage(player, definition);
        }
    }

    private void renderEntryButton(Inventory inventory, Player player) {
        inventory.setItem(13, GuiUtil.createGuiItem(definition.icon(),
                definition.themeColor() + "§l" + definition.displayName(), buildLore(player)));
    }

    private List<String> buildLore(Player player) {
        int highest = manager.getHighestCleared(player, definition);
        int nextStage = definition.nextStage(highest);
        int sweepStage = definition.sweepStage(highest);
        int sweepReward = definition.supportsSweeps() ? manager.getSweepReward(player, definition) : 0;
        double bestDamage = definition.isDamageMeter() ? manager.getBestDamage(player, definition) : 0.0D;
        int sweepsLeft = definition.supportsSweeps() ? manager.getSweepsLeft(player, definition) : 0;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Earn " + definition.rewardName() + " by clearing higher stages.");
        lore.add(definition.isDamageMeter()
                ? ChatColor.GRAY + "Deal as much damage as possible before time expires."
                : ChatColor.GRAY + "Push your best clear, then sweep it for quick rewards.");
        lore.add(" ");
        lore.add(TooltipUtil.sectionHeader("Progress"));
        lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Highest Cleared: " + ChatColor.WHITE + highest));
        lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Next Stage: " + ChatColor.WHITE + nextStage));
        if (definition.supportsSweeps()) {
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Sweeps: " + ChatColor.WHITE + sweepsLeft
                    + ChatColor.GRAY + "/" + ChatColor.WHITE + definition.sweepAttempts()));
        }
        lore.add(" ");
        lore.add(TooltipUtil.sectionHeader("Selected Stage"));
        lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Mob: " + definition.themeColor() + definition.mobDisplayName()));
        if (definition.isDamageMeter()) {
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Objective: " + ChatColor.WHITE + "20s damage meter"));
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Reward Rate: " + definition.themeColor()
                    + "10% of damage dealt " + definition.rewardGlyph()));
        } else {
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "HP: " + ChatColor.WHITE
                    + NumberUtil.formatCommas(Math.round(definition.mobHealth(nextStage)))));
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Clear Reward: " + definition.themeColor()
                    + NumberUtil.formatCommas(definition.rewardForStage(nextStage)) + " " + definition.rewardGlyph()));
        }
        lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Timer: " + ChatColor.WHITE
                + definition.stageTimeSeconds() + "s"));
        if (highest > 0) {
            if (definition.isDamageMeter()) {
                lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Best Damage: " + ChatColor.WHITE
                        + NumberUtil.formatCommas(Math.round(bestDamage))));
            }
            lore.add(TooltipUtil.arrowLine(ChatColor.GRAY + "Sweep Stage: " + ChatColor.WHITE + sweepStage
                    + ChatColor.GRAY + " (" + definition.themeColor()
                    + NumberUtil.formatCommas(sweepReward) + " " + definition.rewardGlyph()
                    + ChatColor.GRAY + ")"));
        } else {
            lore.add(TooltipUtil.arrowLine(ChatColor.RED + "Clear Stage 1 to unlock sweeps."));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to enter the next stage", "to sweep your highest cleared stage"));
        return lore;
    }
}

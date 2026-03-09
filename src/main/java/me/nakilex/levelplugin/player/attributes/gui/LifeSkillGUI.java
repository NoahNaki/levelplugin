package me.nakilex.levelplugin.player.attributes.gui;

import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class LifeSkillGUI {

    public static final String TITLE = "Life Skills";

    private LifeSkillGUI() {}

    public static void open(Player player) {
        player.openInventory(create(player));
    }

    public static Inventory create(Player player) {
        GuiBuilder builder = GuiBuilder.create(45, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        Inventory inventory = builder.build();
        renderWidgets(inventory, player);
        return inventory;
    }

    public static boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = buildWidgets(player).stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private static void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : buildWidgets(player)) {
            widget.contribute(layout, context);
        }
    }

    private static List<GuiWidget> buildWidgets(Player player) {
        MiningManager miningManager = MiningManager.getInstance();
        FarmingManager farmingManager = FarmingManager.getInstance();
        FishingManager fishingManager = FishingManager.getInstance();
        me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager woodcuttingManager = me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager.getInstance();
        List<GuiWidget> widgets = new ArrayList<>();

        widgets.add(new ActionWidget(20,
                context -> createSkillItem(
                        "Mining",
                        Material.DIAMOND_PICKAXE,
                        miningManager.getLevel(context.player()),
                        miningManager.getXP(context.player()),
                        miningManager.getXpRequired(miningManager.getLevel(context.player())),
                        miningManager.getMaxLevel(),
                        TooltipUtil.bulletList(
                                "Improve ore yields and access higher tier nodes.",
                                "Tool bonuses scale with your mining level."
                        )
                ),
                (click, context) -> LifeSkillRewardsGUI.open(context.player(), ToolDiscipline.MINING)));

        widgets.add(new ActionWidget(22,
                context -> createSkillItem(
                        "Fishing",
                        Material.FISHING_ROD,
                        fishingManager.getLevel(context.player()),
                        fishingManager.getXP(context.player()),
                        fishingManager.getXpRequired(fishingManager.getLevel(context.player())),
                        fishingManager.getMaxLevel(),
                        TooltipUtil.bulletList(
                                "Reel in fish during the bite window.",
                                "Higher fishing levels unlock rarer pools."
                        )
                ),
                (click, context) -> LifeSkillRewardsGUI.open(context.player(), ToolDiscipline.FISHING)));

        widgets.add(new ActionWidget(24,
                context -> createSkillItem(
                        "Farming",
                        Material.GOLDEN_HOE,
                        farmingManager.getLevel(context.player()),
                        farmingManager.getXP(context.player()),
                        farmingManager.getXpRequired(farmingManager.getLevel(context.player())),
                        farmingManager.getMaxLevel(),
                        TooltipUtil.bulletList(
                                "Harvest mature crops for the best XP and wheat.",
                                "Higher farming levels improve harvest rewards."
                        )
                ),
                (click, context) -> LifeSkillRewardsGUI.open(context.player(), ToolDiscipline.FARMING)));

        widgets.add(new ActionWidget(31,
                context -> createSkillItem(
                        "Woodcutting",
                        Material.DIAMOND_AXE,
                        woodcuttingManager.getLevel(context.player()),
                        woodcuttingManager.getXP(context.player()),
                        woodcuttingManager.getXpRequired(woodcuttingManager.getLevel(context.player())),
                        woodcuttingManager.getMaxLevel(),
                        TooltipUtil.bulletList(
                                "Cut configured Nexo wood nodes for XP and logs.",
                                "Nodes vanish temporarily, then safely respawn."
                        )
                ),
                (click, context) -> LifeSkillRewardsGUI.open(context.player(), ToolDiscipline.WOODCUTTING)));

        widgets.add(new ActionWidget(40,
                context -> createBackButton(),
                (click, context) -> GuiUtil.openPlayerInventory(context.player())));

        return widgets;
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
                lore.add(ChatColor.GOLD + TooltipUtil.expProgressBarByPixels(xp, displayRequired, 156));
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

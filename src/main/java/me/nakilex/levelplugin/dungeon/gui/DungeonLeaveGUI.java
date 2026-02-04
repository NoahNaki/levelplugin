package me.nakilex.levelplugin.dungeon.gui;

import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Confirmation GUI shown when a player attempts to leave a dungeon instance. */
public class DungeonLeaveGUI implements Listener {
    private static final String TITLE = "Leave Dungeon?";
    private static final int SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final DungeonManager dungeonManager;
    private final List<GuiWidget> widgets;

    public DungeonLeaveGUI(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        if (player == null) return;

        if (!dungeonManager.isInstanceWorld(player.getWorld())) {
            dungeonManager.getPlugin().getDungeonListGUI().open(player);
            return;
        }

        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        renderWidgets(inv, player);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        handleWidgetClick(event, player);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(CONFIRM_SLOT,
                context -> createConfirmItem(),
                (click, context) -> {
                    Player player = context.player();
                    player.closeInventory();
                    dungeonManager.handleInstanceExit(player.getWorld(), player, true);
                }));
        widgetList.add(new ActionWidget(CANCEL_SLOT,
                context -> createCancelItem(),
                (click, context) -> context.player().closeInventory()));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private ItemStack createConfirmItem() {
        ItemStack item = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm Exit");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Return to your last location.");
            lore.addAll(TooltipUtil.clickInstructions("to leave the dungeon", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = GuiUtil.getNexoItem("cross", ChatColor.RED + "Stay Here");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Keep exploring and collect your loot.");
            lore.addAll(TooltipUtil.clickInstructions("to close", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

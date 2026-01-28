package me.nakilex.levelplugin.catacombs;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.widgets.NexoButtonWidget;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple entry GUI for the Catacombs flow to keep UX consistent
 * with existing expedition menus.
 */
public class CatacombsGUI implements Listener {
    private static final int SIZE = 27;
    private static final String TITLE = "Catacombs";

    private final CatacombsManager manager;
    private final PlayerConfig playerConfig = Main.getInstance().getPlayerConfig();
    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final List<GuiWidget> widgets;

    public CatacombsGUI(CatacombsManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(true)
                .build();

        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        handleWidgetClick(event, player);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new NexoButtonWidget(13, "portal", ChatColor.GOLD + "Enter the Catacombs",
                context -> buildEntryLore(context.player()),
                (click, context) -> {
                    context.player().closeInventory();
                    manager.startRun(context.player());
                }));
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
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private List<String> buildEntryLore(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Fight through an endless chain of rooms.");
        lore.addAll(TooltipUtil.bulletList(
                "Scale up difficulty as you clear stages",
                "Timed encounters reward fast clears",
                "Return to the last waiting room if you fail"
        ));
        lore.add(" ");
        Integer slot = profileManager.getActiveSlot(player.getUniqueId());
        int bestStage = slot == null ? 0 : playerConfig.getCatacombsBestStage(player.getUniqueId(), slot);
        lore.add(ChatColor.GRAY + "Highest Cleared: " + ChatColor.WHITE + bestStage);
        lore.add(ChatColor.GRAY + "Next Stage: " + ChatColor.WHITE + Math.max(1, bestStage + 1));
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Status: " + (manager.isInCatacombs(player.getUniqueId())
                ? ChatColor.GREEN + "In progress"
                : ChatColor.YELLOW + "Ready"));
        lore.addAll(TooltipUtil.clickInstructions("to enter the Catacombs", null));
        return lore;
    }
}

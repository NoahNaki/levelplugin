package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** GUI for investing materials into settlement upgrades. */
public class UpgradeGUI implements Listener {
    private static final String TITLE = "Settlement Upgrade";
    private static final int INVEST_SLOT = 13;
    private final EnvironmentManager manager;
    private final List<GuiWidget> widgets;

    public UpgradeGUI(EnvironmentManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        renderWidgets(inv, p);
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        handleWidgetClick(e, player);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(INVEST_SLOT,
                context -> createInvestItem(),
                (click, context) -> handleInvestClick(context.player())));
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

    private ItemStack createInvestItem() {
        return GuiUtil.createGuiItem(Material.OAK_LOG,
                ChatColor.GREEN + "Invest 1 Oak Log",
                List.of(
                        ChatColor.GRAY + "Click to invest towards",
                        ChatColor.GRAY + "the next upgrade."
                ));
    }

    private void handleInvestClick(Player player) {
        if (player.getInventory().contains(Material.OAK_LOG)) {
            player.getInventory().removeItem(new ItemStack(Material.OAK_LOG, 1));
            manager.invest(player, 1);
            open(player);
        } else {
            player.sendMessage(ChatColor.RED + "You need an oak log to invest!");
        }
    }
}

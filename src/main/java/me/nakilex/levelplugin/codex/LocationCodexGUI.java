package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.codex.CodexGuiUtil;
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

import java.util.*;

/** GUI listing discovered locations. */
public class LocationCodexGUI implements Listener {
    private static final String TITLE = "Codex - Locations";

    private final CodexManager manager;
    private CodexMainGUI mainGui;
    private final Map<java.util.UUID, Integer> backSlots = new HashMap<>();
    private List<GuiWidget> widgets = new ArrayList<>();

    public LocationCodexGUI(CodexManager manager, CodexMainGUI mainGui) {
        this.manager = manager;
        this.mainGui = mainGui;
    }

    public void setMainGui(CodexMainGUI gui) { this.mainGui = gui; }

    public void open(Player player) {
        List<String> list = new ArrayList<>(manager.getDiscoveredLocations(player.getUniqueId()));
        int size = ((list.size() - 1) / 9 + 1) * 9;
        Inventory inv = GuiBuilder.create(Math.max(size, 27), TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();

        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Locations", String.valueOf(list.size()));
        inv.setItem(4, CodexGuiUtil.createInfoBook("Discoveries", lines));

        int slot = 0;
        for (String name : list) {
            if (slot == 4) slot++; // skip info book slot
            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + name);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        int backSlot = inv.getSize() - 1;
        backSlots.put(player.getUniqueId(), backSlot);
        widgets = buildWidgets(backSlot);
        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (handleWidgetClick(e, player)) {
            return;
        }
        e.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets(int backSlot) {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(backSlot,
                context -> GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"),
                (click, context) -> {
                    if (mainGui != null) {
                        mainGui.open(context.player());
                    }
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
        int backSlot = backSlots.getOrDefault(player.getUniqueId(), -1);
        if (slot != backSlot) {
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
}

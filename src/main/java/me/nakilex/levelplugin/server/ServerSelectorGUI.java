package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
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

public class ServerSelectorGUI implements Listener {
    private static final String TITLE = "Server Selector";
    private static final int ALPHA_SLOT = 11;
    private static final int BUILD_SLOT = 15;

    private final ServerSelectionManager manager;
    private final List<GuiWidget> widgets;

    public ServerSelectorGUI(ServerSelectionManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        Inventory gui = GuiBuilder.create(27, TITLE)
                .fillEmptySlots(false)
                .build();
        renderWidgets(gui, player);
        player.openInventory(gui);
    }

    private ItemStack createAlphaItem(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Alpha Test");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(TooltipUtil.bulletList(
                    "Play the full MMORPG experience.",
                    "Create or select a profile to begin."
            ));
            lore.add("");
            lore.addAll(TooltipUtil.clickInstructions("to connect", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBuildItem(Player player) {
        boolean allowed = manager.canAccessBuild(player);
        Material icon = allowed ? Material.BRICKS : Material.BARRIER;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Build Server");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(TooltipUtil.bulletList(
                    "Access the flatland build world.",
                    "LevelPlugin features are disabled here."
            ));
            lore.add("");
            if (allowed) {
                lore.addAll(TooltipUtil.clickInstructions("to connect", null));
            } else {
                lore.add(ChatColor.RED + "Staff-only access.");
                lore.add(ChatColor.GRAY + "Requires permission or weight "
                        + manager.getBuildMinWeight() + "+.");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        handleWidgetClick(event, player);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(ALPHA_SLOT,
                context -> createAlphaItem(context.player()),
                (click, context) -> {
                    context.player().closeInventory();
                    manager.sendToAlpha(context.player());
                }));
        widgetList.add(new ActionWidget(BUILD_SLOT,
                context -> createBuildItem(context.player()),
                (click, context) -> handleBuildClick(context.player())));
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

    private void handleBuildClick(Player player) {
        if (!manager.canAccessBuild(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You do not have access to the build server.");
            return;
        }
        player.closeInventory();
        manager.sendToBuild(player);
    }
}

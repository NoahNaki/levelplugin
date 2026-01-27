package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobCodexGUI implements Listener {
    // Use a unique title so our click listener doesn't interfere with the
    // main codex menu which also uses "Codex" as its title.
    private static final String TITLE = "Codex - Mobs";
    private static final int SIZE = 54;

    private static final int ITEMS_PER_PAGE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 49;

    private final CodexManager manager;
    private CodexMainGUI mainGui;
    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final List<GuiWidget> widgets;

    public MobCodexGUI(CodexManager manager, CodexMainGUI mainGui) {
        this.manager = manager;
        this.mainGui = mainGui;
        this.widgets = buildWidgets();
    }

    public void setMainGui(CodexMainGUI gui) { this.mainGui = gui; }

    public void open(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Mobs", manager.getDiscoveredMobCount(player.getUniqueId()) + "/" + manager.getTotalMobCount());
        inv.setItem(4, CodexGuiUtil.createInfoBook("Discoveries", lines));

        List<String> mobs = manager.getAllMobKeys();
        int start = page * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < mobs.size() && slot < ITEMS_PER_PAGE; i++) {
            String key = mobs.get(i);
            inv.setItem(GuiUtil.PAGED_SLOTS[slot++], createMobIcon(player.getUniqueId(), key));
        }

        renderWidgets(inv, player);

        player.openInventory(inv);
    }

    private ItemStack createMobIcon(UUID id, String key) {
        boolean discovered = manager.hasDiscovered(id, key);
        boolean isFieldBoss = manager.isFieldBoss(key);
        Material iconMaterial = Material.GRAY_DYE;
        if (discovered) {
            iconMaterial = isFieldBoss ? Material.WITHER_SKELETON_SKULL : Material.SKELETON_SKULL;
        }
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (discovered) {
                String name = MobNameUtil.getPlainDisplayName(key);
                meta.setDisplayName(ChatColor.GREEN + name);
                meta.setLore(CodexGuiUtil.mobProgressLore(manager, id, key));
            } else {
                meta.setDisplayName(ChatColor.DARK_GRAY + "???");
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (handleWidgetClick(e, p)) {
            return;
        }
        e.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new java.util.ArrayList<>();
        widgetList.add(new ActionWidget(PREV_SLOT,
                context -> createPrevItem(context.player()),
                (click, context) -> handlePrev(context.player())));
        widgetList.add(new ActionWidget(NEXT_SLOT,
                context -> createNextItem(context.player()),
                (click, context) -> handleNext(context.player())));
        widgetList.add(new ActionWidget(BACK_SLOT,
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

    private ItemStack createPrevItem(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        return page > 0 ? GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous") : null;
    }

    private ItemStack createNextItem(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        int total = manager.getAllMobKeys().size();
        return total > (page + 1) * ITEMS_PER_PAGE
                ? GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next")
                : null;
    }

    private void handlePrev(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, Math.max(0, page - 1));
    }

    private void handleNext(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page + 1);
    }
}

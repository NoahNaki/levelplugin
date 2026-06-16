package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.HeadUtil;
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

import java.util.List;
import java.util.function.Consumer;

/** Main Codex menu providing category selection. */
public class CodexMainGUI implements Listener {
    private static final String TITLE = "Codex";
    private static final int SIZE = 27;

    // base64 textures for category icons
    private static final String LOC_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmVlZjdlNTZjZGU3NDA3NzJkZmI3NmRkZDJmNTg0YmU4OTA3Yjg1OTc2NjhlNDAyNjM0OTg2NDY5MjMwYWE0OSJ9fX0=";
    private static final String NPC_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU3NTM0NzBlNjdlMzUwZGI2MDVhOTFmNDNhNmYxODJlZmY3NTlkNmI4ZThmNTY0MWVlYjdkNmViYjYxN2JlYyJ9fX0=";
    private static final String MOB_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWM1OGExMDYyMzZjMjM4MGI2MTEzZGY4NDhkZDAxN2I2OWFiYWZmYTQ5M2RhNjkyNzA4MTMyZjBiMjcyMTI3OCJ9fX0=";
    private static final String FOOD_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzQ4NDU5NzZkNDQ1Y2NmOTI3Y2I4YmY5ZDljZjY3ZDI5NDE0Nzc2NjMzYzQ1MWQ1MWUyZWJkY2U0OGE3NSJ9fX0=";

    private final MobCodexGUI mobGui;
    private final NpcCodexGUI npcGui;
    private final LocationCodexGUI locGui;
    private final FoodCodexGUI foodGui;
    private final List<GuiWidget> widgets;

    public CodexMainGUI(MobCodexGUI m, NpcCodexGUI n, LocationCodexGUI l, FoodCodexGUI f) {
        this.mobGui = m;
        this.npcGui = n;
        this.locGui = l;
        this.foodGui = f;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    public void openFrom(Player player, Consumer<Player> backAction) {
        // Kept for API compatibility with callers that provide a back action.
        open(player);
    }

    private ItemStack createHead(String b64, String name) {
        return HeadUtil.createCustomHead(b64, name, null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;
        handleWidgetClick(e, player);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new java.util.ArrayList<>();
        widgetList.add(new ActionWidget(10,
                context -> createHead(LOC_HEAD, ChatColor.GREEN + "Locations"),
                (click, context) -> locGui.open(context.player())));
        widgetList.add(new ActionWidget(12,
                context -> createHead(NPC_HEAD, ChatColor.YELLOW + "NPCs"),
                (click, context) -> npcGui.open(context.player())));
        widgetList.add(new ActionWidget(14,
                context -> createHead(FOOD_HEAD, ChatColor.GOLD + "Foods"),
                (click, context) -> foodGui.open(context.player())));
        widgetList.add(new ActionWidget(16,
                context -> createHead(MOB_HEAD, ChatColor.RED + "Mobs"),
                (click, context) -> mobGui.open(context.player())));
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
}

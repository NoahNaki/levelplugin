package me.nakilex.levelplugin.pet.gui;

import me.nakilex.levelplugin.pet.summon.PetSummonManager;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetSummonGUI implements Listener {
    private static final int GUI_SIZE = 27;
    private static final int SINGLE_SLOT = 11;
    private static final int TEN_SLOT = 15;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private final PetSummonManager summonManager;
    private final String title = ChatUtil.applyEmojis("§8Pet Summon");
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public PetSummonGUI(PetSummonManager summonManager) {
        this.summonManager = summonManager;
    }

    public void open(Player player) {
        Inventory inventory = GuiBuilder.create(GUI_SIZE, title)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inventory, player, widgets);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String viewTitle = LEGACY.serialize(event.getView().title());
        if (!GuiUtil.titleMatches(viewTitle, title)) {
            return;
        }
        if (!handleWidgetClick(event, player)) {
            event.setCancelled(true);
        }
    }

    private List<GuiWidget> buildWidgets(Player player) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(SINGLE_SLOT, ctx -> createOptionItem(
                        Material.NETHER_STAR,
                        "§a1x Pet Pull",
                        "Summon 1 random pet", PetSummonManager.summonCostForAmount(1)),
                (click, context) -> handleSummon(player, 1)));
        widgets.add(new ActionWidget(TEN_SLOT, ctx -> createOptionItem(
                        Material.BEACON,
                        "§b10x Pet Pull",
                        "Summon 10 random pets", PetSummonManager.summonCostForAmount(10)),
                (click, context) -> handleSummon(player, 10)));
        return widgets;
    }

    private ItemStack createOptionItem(Material material, String name, String action, int cost) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList(action, "Cost: " + cost + " <glyph:purple_orb_icon>"));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to confirm", null));
        return GuiUtil.createGuiItem(material, name, lore);
    }

    private void handleSummon(Player player, int amount) {
        player.closeInventory();
        summonManager.startSummon(player, amount);
    }

    private void renderWidgets(Inventory inv, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inv);
        GuiContext context = new GuiContext(player, inv);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
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

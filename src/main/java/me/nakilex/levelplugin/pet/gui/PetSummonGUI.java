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
    private static final int INFO_SLOT = 8;
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
                        player, Material.NETHER_STAR,
                        "§a1x Pet Pull",
                        "Summon §e1§7 random pet", PetSummonManager.summonCostForAmount(1)),
                (click, context) -> handleSummon(player, 1)));
        widgets.add(new ActionWidget(TEN_SLOT, ctx -> createOptionItem(
                        player, Material.BEACON,
                        "§b10x Pet Pull",
                        "Summon §e10§7 random pets", PetSummonManager.summonCostForAmount(10)),
                (click, context) -> handleSummon(player, 10)));
        widgets.add(new ActionWidget(INFO_SLOT, ctx -> createRatesInfoItem(player), (click, context) -> {}));
        return widgets;
    }

    private ItemStack createOptionItem(Player player, Material material, String name, String action, int cost) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList(action, "Cost: §d" + cost + " <glyph:purple_orb_icon>"));
        lore.add(" ");
        lore.addAll(buildPityLore(player));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to confirm", null));
        return GuiUtil.createGuiItem(material, name, lore);
    }


    private List<String> buildPityLore(Player player) {
        List<String> lore = new ArrayList<>();
        int threshold = summonManager.getPityThreshold();
        int current = summonManager.getPityPullsSinceLegendary(player.getUniqueId());
        int clamped = Math.max(0, Math.min(current, threshold));
        int remaining = Math.max(0, threshold - clamped);
        lore.add("§ePity Progress");
        String bar = TooltipUtil.expProgressBarByPixels(clamped, Math.max(1, threshold), 156);
        lore.add(bar + " " + org.bukkit.ChatColor.GRAY + clamped
                + org.bukkit.ChatColor.GOLD + "/" + org.bukkit.ChatColor.GRAY + threshold
                + " <glyph:experience_orb_icon>");
        if (remaining > 0) {
            lore.add("§7Guaranteed Legendary+ in §e" + remaining + "§7 pull(s)");
        } else {
            lore.add("§aGuaranteed Legendary+ on next pull");
        }
        return lore;
    }

    private ItemStack createRatesInfoItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7Base pull rates:");
        var rates = summonManager.getGachaRates();
        for (var rarity : summonManager.getGachaRarities()) {
            String name = rarity.name().toLowerCase(java.util.Locale.ROOT);
            String label = name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1);
            lore.add("§7• " + rarity.getColor() + label + "§7: §f" + String.format("%.1f", rates.getOrDefault(rarity, 0.0)) + "%");
        }
        lore.add(" ");
        lore.add("§7Pity: §f" + summonManager.getPityThreshold() + " pulls");
        lore.add("§7Legendary+ resets pity counter.");
        lore.add(" ");
        lore.addAll(buildPityLore(player));
        return GuiUtil.getNexoItem("info", "§eSummon Rates", lore);
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

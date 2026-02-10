package me.nakilex.levelplugin.pet.gui;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.data.PetProfile;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetSettingsGUI implements Listener {
    private static final int GUI_SIZE = 45;
    private static final int AUTO_DISCARD_SLOT = 20;
    private static final int AUTO_SKIP_SUMMON_SLOT = 24;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PetManager petManager;
    private final String title = ChatUtil.applyEmojis("§8Pet Settings");
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new java.util.HashMap<>();
    private PetGUI petGUI;

    public PetSettingsGUI(PetManager petManager) {
        this.petManager = petManager;
    }

    public void setPetGUI(PetGUI petGUI) {
        this.petGUI = petGUI;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, title)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
        player.openInventory(inv);
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
        PetProfile profile = petManager.getProfile(player.getUniqueId());
        widgets.add(new ActionWidget(0, ctx -> GuiUtil.getNexoItem("arrow_left2", "§7Back"),
                (click, context) -> {
                    if (petGUI != null) {
                        petGUI.open(player, 0);
                    }
                }));
        widgets.add(new ActionWidget(AUTO_DISCARD_SLOT, ctx -> createAutoDiscardItem(profile),
                (click, context) -> cycleAutoDiscard(profile, click.isLeftClick(), context.player())));
        widgets.add(new ActionWidget(AUTO_SKIP_SUMMON_SLOT, ctx -> createAutoSkipSummonItem(profile),
                (click, context) -> toggleAutoSkipSummon(profile, context.player())));
        return widgets;
    }

    private ItemStack createAutoDiscardItem(PetProfile profile) {
        ItemRarity active = profile.autoDiscardRarity();
        ItemStack item = GuiUtil.getRarityArrowItem(active == null ? ItemRarity.COMMON : active,
                "§bAuto Discard Filter");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Discard pulled pets at or below");
            lore.add("§7the selected rarity.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(active == null, "Off"));
            for (ItemRarity rarity : petManager.getGachaRarities()) {
                String label = rarity.getColor() + formatRarityLabel(rarity);
                lore.add(TooltipUtil.selectionLine(rarity == active, label));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }


    private ItemStack createAutoSkipSummonItem(PetProfile profile) {
        boolean enabled = profile.autoSkipSummonAnimation();
        ItemStack item = GuiUtil.createGuiItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "§dAuto Skip Pull Animation",
                List.of());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Automatically skip the summon");
            lore.add("§7cutscene/animation after pulling.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(enabled, "On"));
            lore.add(TooltipUtil.selectionLine(!enabled, "Off"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to toggle", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void toggleAutoSkipSummon(PetProfile profile, Player player) {
        profile.setAutoSkipSummonAnimation(!profile.autoSkipSummonAnimation());
        open(player);
    }

    private String formatRarityLabel(ItemRarity rarity) {
        String name = rarity.name().toLowerCase(java.util.Locale.ROOT);
        return name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1);
    }

    private void cycleAutoDiscard(PetProfile profile, boolean forward, Player player) {
        List<ItemRarity> options = petManager.getGachaRarities();
        ItemRarity current = profile.autoDiscardRarity();
        int index = current == null ? -1 : options.indexOf(current);
        int next = forward ? index + 1 : index - 1;
        ItemRarity updated;
        if (next >= options.size()) {
            updated = null;
        } else if (next < -1) {
            updated = options.get(options.size() - 1);
        } else if (next == -1) {
            updated = null;
        } else {
            updated = options.get(next);
        }
        profile.setAutoDiscardRarity(updated);
        open(player);
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

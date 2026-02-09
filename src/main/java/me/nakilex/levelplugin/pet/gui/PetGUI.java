package me.nakilex.levelplugin.pet.gui;

import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetProgression;
import me.nakilex.levelplugin.pet.utils.PetGuiUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PetGUI implements Listener {
    private static final int GUI_SIZE = 54;
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PetManager petManager;
    private final String title = ChatUtil.applyEmojis("§8Pets");
    private final Map<UUID, Integer> pages = new java.util.HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new java.util.HashMap<>();

    public PetGUI(PetManager petManager) {
        this.petManager = petManager;
    }

    public void open(Player player, int page) {
        List<PetDefinition> defs = petManager.getPetIds().stream()
                .map(petManager::getDefinition)
                .flatMap(Optional::stream)
                .toList();
        int maxPage = Math.max(0, (defs.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), current);

        Inventory inv = GuiBuilder.create(GUI_SIZE, title)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildPetWidgets(player, defs, current, maxPage);
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
        if (handleWidgetClick(event, player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == PREV_SLOT) {
            int page = pages.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) {
                open(player, page - 1);
            }
            return;
        }
        if (slot == NEXT_SLOT) {
            int page = pages.getOrDefault(player.getUniqueId(), 0);
            open(player, page + 1);
            return;
        }
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        String petId = resolvePetId(slot, player);
        if (petId == null) {
            return;
        }
        boolean equipped = petManager.getProfile(player.getUniqueId()).activePetId() != null
                && petManager.getProfile(player.getUniqueId()).activePetId().equalsIgnoreCase(petId);
        switch (event.getClick()) {
            case LEFT, SHIFT_LEFT -> petManager.summonPet(player, petId);
            case RIGHT, SHIFT_RIGHT -> {
                if (equipped) {
                    petManager.dismissPet(player);
                }
            }
            default -> {
            }
        }
        refresh(player, event.getView().getTopInventory());
    }

    private List<GuiWidget> buildPetWidgets(Player player, List<PetDefinition> defs, int page, int maxPage) {
        List<GuiWidget> widgets = new ArrayList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(defs.size(), start + PAGE_SIZE);
        String activeId = petManager.getProfile(player.getUniqueId()).activePetId();
        for (int i = start; i < end; i++) {
            PetDefinition def = defs.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            int xp = petManager.getProfile(player.getUniqueId()).getPetXp(def.id());
            int level = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
            Map<StatType, Integer> stats = def.statsForLevel(level);
            List<PetEffectDefinition> effects = def.effectsForLevel(level);
            boolean equipped = activeId != null && activeId.equalsIgnoreCase(def.id());
            ItemStack icon = PetGuiUtil.createPetIcon(def, level, xp, stats, effects, equipped);
            String petId = def.id();
            widgets.add(new ActionWidget(slot, ctx -> icon, (click, context) -> {
                if (click.isRightClick() && equipped) {
                    petManager.dismissPet(player);
                } else if (click.isLeftClick()) {
                    petManager.summonPet(player, petId);
                }
                refresh(player, context.inventory());
            }));
        }

        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT, ctx -> createNavItem("§aPrevious Page"),
                    (click, context) -> open(player, page - 1)));
        }
        if (page < maxPage) {
            widgets.add(new ActionWidget(NEXT_SLOT, ctx -> createNavItem("§aNext Page"),
                    (click, context) -> open(player, page + 1)));
        }
        return widgets;
    }

    private ItemStack createNavItem(String name) {
        List<String> lore = TooltipUtil.clickInstructions("to change page", null);
        return GuiUtil.createGuiItem(Material.ARROW, name, lore);
    }

    private void renderWidgets(Inventory inv, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inv);
        GuiContext context = new GuiContext(player, inv);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private void refresh(Player player, Inventory inventory) {
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        List<PetDefinition> defs = petManager.getPetIds().stream()
                .map(petManager::getDefinition)
                .flatMap(Optional::stream)
                .toList();
        int maxPage = Math.max(0, (defs.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), current);
        List<GuiWidget> widgets = buildPetWidgets(player, defs, current, maxPage);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inventory, player, widgets);
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

    private String resolvePetId(int slot, Player player) {
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int index = -1;
        for (int i = 0; i < GuiUtil.PAGED_SLOTS.length; i++) {
            if (GuiUtil.PAGED_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return null;
        }
        int globalIndex = page * PAGE_SIZE + index;
        List<String> ids = petManager.getPetIds();
        if (globalIndex >= ids.size()) {
            return null;
        }
        return ids.get(globalIndex);
    }
}

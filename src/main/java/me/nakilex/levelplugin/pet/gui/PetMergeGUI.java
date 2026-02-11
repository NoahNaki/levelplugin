package me.nakilex.levelplugin.pet.gui;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetFeedbackUtil;
import me.nakilex.levelplugin.pet.utils.PetPullSummaryUtil;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PetMergeGUI implements Listener {
    private static final int GUI_SIZE = 54;
    private static final int CONFIRM_SIZE = 27;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PetManager petManager;
    private final String mergeTitle = ChatUtil.applyEmojis("§8Pet Merge");
    private final String selectTitle = ChatUtil.applyEmojis("§8Select Pets to Merge");
    private final String confirmTitle = ChatUtil.applyEmojis("§8Confirm Merge");
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private final Map<UUID, Integer> pageByPlayer = new HashMap<>();
    private final Map<UUID, ItemRarity> filterByPlayer = new HashMap<>();
    private final Map<UUID, SortMode> sortByPlayer = new HashMap<>();
    private final Map<UUID, LinkedHashSet<String>> selectedEntryKeys = new HashMap<>();
    private final Map<UUID, List<CopyEntry>> visibleEntries = new HashMap<>();
    private final Map<UUID, Map<String, CopyEntry>> entryIndexByPlayer = new HashMap<>();
    private PetGUI petGUI;

    public PetMergeGUI(PetManager petManager) {
        this.petManager = petManager;
    }

    public void setPetGUI(PetGUI petGUI) {
        this.petGUI = petGUI;
    }

    public void openMerge(Player player) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, mergeTitle)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildMergeWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(inv, player, widgets);
        player.openInventory(inv);
        PetFeedbackUtil.playMenuOpen(player);
    }

    private List<GuiWidget> buildMergeWidgets(Player player) {
        List<GuiWidget> widgets = new ArrayList<>();
        int[] slots = {20, 21, 22, 23, 24};
        List<String> ids = selectedPetIds(player);
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            int idx = i;
            widgets.add(new ActionWidget(slot, ctx -> mergeSlotItem(player, idx),
                    (click, context) -> openSelect(context.player(), pageByPlayer.getOrDefault(player.getUniqueId(), 0))));
        }
        widgets.add(new ActionWidget(49, ctx -> mergeButton(player), (click, context) -> openConfirm(context.player())));
        widgets.add(new ActionWidget(48, ctx -> mergeAllButton(), (click, context) -> mergeAllDuplicates(context.player())));
        widgets.add(new ActionWidget(45, ctx -> GuiUtil.getNexoItem("arrow_left", "§eBack"),
                (click, context) -> { if (petGUI != null) petGUI.open(context.player(), 0);}));
        widgets.add(new ActionWidget(50, ctx -> GuiUtil.getNexoItem("plus", "§bSelect Pets", TooltipUtil.clickInstructions("to open selection", null)),
                (click, context) -> openSelect(context.player(), 0)));
        return widgets;
    }

    private ItemStack mergeSlotItem(Player player, int index) {
        List<String> ids = selectedPetIds(player);
        if (index >= ids.size()) {
            return GuiUtil.getNexoItem("cross", "§7Empty Slot", TooltipUtil.clickInstructions("to select a pet", null));
        }
        String petId = ids.get(index);
        PetDefinition def = petManager.getDefinition(petId).orElse(null);
        if (def == null) return GuiUtil.getNexoItem("cross", "§7Empty Slot");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7Selected: " + def.rarity().getColor() + def.displayName());
        lore.addAll(TooltipUtil.clickInstructions("to edit selection", null));
        return GuiUtil.getNexoItem("check", "§aSelected", lore);
    }

    private ItemStack mergeButton(Player player) {
        sanitizeSelected(player);
        List<String> ids = selectedPetIds(player);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        if (ids.isEmpty()) {
            lore.addAll(TooltipUtil.bulletList("Select up to 5 pets to merge."));
        } else {
            int chance = Math.min(100, ids.size() * 20);
            ItemRarity baseRarity = petManager.getPetRarity(ids.get(0));
            ItemRarity upgradedRarity = nextRarity(baseRarity);
            ChatColor chanceColor = chanceColor(chance);
            lore.addAll(TooltipUtil.bulletList(
                    "Selected pets: §f" + ids.size() + "§7/§f5",
                    "Success chance: " + chanceColor + "● §f" + chance + "§7%",
                    "Success gives " + upgradedRarity.getColor() + upgradedRarity.name() + "§7 pet",
                    "Fail gives " + baseRarity.getColor() + baseRarity.name() + "§7 pet"));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to open merge confirmation", null));
        return GuiUtil.getNexoItem("check", "§aMerge Pets", lore);
    }

    private ItemStack mergeAllButton() {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList(
                "Merges all duplicate pets in groups of 5",
                "Locked pets are skipped",
                "Shows acquired pets in chat"));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to merge all duplicates", null));
        return GuiUtil.getNexoItem("server_icon", "§dMerge All Duplicates", lore);
    }

    private void openSelect(Player player, int page) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, selectTitle)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        int current = Math.max(0, page);
        pageByPlayer.put(player.getUniqueId(), current);
        List<GuiWidget> widgets = buildSelectWidgets(player, current);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(inv, player, widgets);
        player.openInventory(inv);
        PetFeedbackUtil.playMenuOpen(player);
    }

    private List<GuiWidget> buildSelectWidgets(Player player, int page) {
        UUID id = player.getUniqueId();
        List<GuiWidget> widgets = new ArrayList<>();
        List<CopyEntry> all = buildCopies(player, filterByPlayer.getOrDefault(id, null), sortByPlayer.getOrDefault(id, SortMode.DATE_ACQUIRED));
        visibleEntries.put(id, all);
        sanitizeSelected(player);
        int maxPage = Math.max(0, (all.size() - 1) / GuiUtil.PAGED_SLOTS.length);
        int current = Math.min(page, maxPage);
        pageByPlayer.put(id, current);
        int start = current * GuiUtil.PAGED_SLOTS.length;
        int end = Math.min(all.size(), start + GuiUtil.PAGED_SLOTS.length);
        for (int i = start; i < end; i++) {
            CopyEntry entry = all.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            widgets.add(new ActionWidget(slot, ctx -> selectionItem(player, entry),
                    (click, context) -> handleSelectionClick(player, entry, click)));
        }
        if (current > 0) widgets.add(new ActionWidget(45, ctx -> navItem(false), (click, context) -> openSelect(player, current - 1)));
        if (current < maxPage) widgets.add(new ActionWidget(53, ctx -> navItem(true), (click, context) -> openSelect(player, current + 1)));

        widgets.add(new ActionWidget(50, ctx -> sortButton(player), (click, context) -> {
            SortMode next = SortMode.next(sortByPlayer.getOrDefault(id, SortMode.DATE_ACQUIRED), click.isLeftClick());
            sortByPlayer.put(id, next);
            openSelect(player, 0);
        }));
        widgets.add(new ActionWidget(51, ctx -> filterButton(player), (click, context) -> {
            filterByPlayer.put(id, nextFilter(filterByPlayer.get(id), click.isLeftClick()));
            openSelect(player, 0);
        }));
        widgets.add(new ActionWidget(49, ctx -> selectionInfo(player), null));
        widgets.add(new ActionWidget(48, ctx -> GuiUtil.getNexoItem("arrow_left", "§eBack", TooltipUtil.clickInstructions("to return to merge menu", null)),
                (click, context) -> openMerge(player)));
        return widgets;
    }

    private void handleSelectionClick(Player player, CopyEntry entry, ClickType click) {
        UUID id = player.getUniqueId();
        if (click.isShiftClick() && click.isRightClick()) {
            boolean locked = !petManager.isPetLocked(player.getUniqueId(), entry.petId());
            petManager.setPetLocked(player.getUniqueId(), entry.petId(), locked);
            PetFeedbackUtil.playLockToggle(player, locked);
            PetChatUtil.send(player, (locked ? "Locked " : "Unlocked ") + entry.displayName() + " from merge.");
            PetFeedbackUtil.playMenuSelect(player);
            openSelect(player, pageByPlayer.getOrDefault(id, 0));
            return;
        }
        LinkedHashSet<String> selected = selectedEntryKeys.computeIfAbsent(id, k -> new LinkedHashSet<>());
        if (selected.contains(entry.entryKey())) {
            selected.remove(entry.entryKey());
        } else {
            if (selected.size() >= 5) {
                PetChatUtil.send(player, "You can only select up to 5 pets.");
                return;
            }
            ItemRarity selectedRarity = selectedRarity(player, selected);
            if (selectedRarity != null && selectedRarity != entry.rarity()) {
                PetChatUtil.send(player, "All selected pets must be the same rarity.");
                return;
            }
            selected.add(entry.entryKey());
        }
        PetFeedbackUtil.playMenuSelect(player);
        openSelect(player, pageByPlayer.getOrDefault(id, 0));
    }

    private List<CopyEntry> buildCopies(Player player, ItemRarity filter, SortMode sortMode) {
        List<CopyEntry> list = new ArrayList<>();
        var profile = petManager.getProfile(player.getUniqueId());
        List<PetDefinition> owned = petManager.getOwnedPets(player.getUniqueId());
        for (PetDefinition def : owned) {
            if (filter != null && def.rarity() != filter) continue;
            int copies = profile.getPetCopies(def.id());
            int level = me.nakilex.levelplugin.pet.PetProgression.levelFromXp(profile.getPetXp(def.id()), def.xpPerLevel(), def.maxLevel());
            List<Long> history = profile.getPetCopyAcquiredHistory(def.id());
            for (int i = 0; i < copies; i++) {
                long acquiredAt = i < history.size() ? history.get(i) : profile.getLastAcquiredAt(def.id());
                list.add(new CopyEntry(def.id() + "#" + i, def.id(), def.displayName(), def.rarity(), level, acquiredAt));
            }
        }
        Comparator<CopyEntry> comparator = switch (sortMode) {
            case LEVEL -> Comparator.comparingInt(CopyEntry::level).reversed();
            case RARITY -> Comparator.comparingInt((CopyEntry e) -> e.rarity().ordinal()).reversed();
            case DATE_ACQUIRED -> Comparator.comparingLong(CopyEntry::acquiredAt).reversed();
        };
        list.sort(comparator.thenComparing(CopyEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        Map<String, CopyEntry> entryIndex = new HashMap<>();
        for (CopyEntry entry : list) {
            entryIndex.put(entry.entryKey(), entry);
        }
        entryIndexByPlayer.put(player.getUniqueId(), entryIndex);
        return list;
    }

    private ItemStack selectionItem(Player player, CopyEntry entry) {
        UUID id = player.getUniqueId();
        boolean selected = selectedEntryKeys.getOrDefault(id, new LinkedHashSet<>()).contains(entry.entryKey());
        boolean locked = petManager.isPetLocked(id, entry.petId());
        String icon = selected ? "check" : (locked ? "lock" : "plus");
        String name = (selected ? "§aSelected " : "§b") + entry.displayName();
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7Rarity: " + entry.rarity().getColor() + entry.rarity().name());
        lore.add("§7Level: §f" + entry.level());
        lore.add("§7Status: " + (selected ? "§aSelected" : locked ? "§cLocked" : "§7Not selected"));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions(selected ? "to unselect" : "to select", null));
        lore.addAll(TooltipUtil.sneakClickInstructions(null, locked ? "to unlock this pet" : "to lock this pet"));
        return GuiUtil.getNexoItem(icon, name, lore);
    }

    private ItemStack navItem(boolean next) {
        String name = next ? "§aNext Page" : "§aPrevious Page";
        String id = next ? "arrow_right" : "arrow_left";
        return GuiUtil.getNexoItem(id, name, TooltipUtil.clickInstructions("to change page", null));
    }

    private ItemStack sortButton(Player player) {
        SortMode mode = sortByPlayer.getOrDefault(player.getUniqueId(), SortMode.DATE_ACQUIRED);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        for (SortMode value : SortMode.values()) {
            lore.add(TooltipUtil.selectionLine(value == mode, value.label));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.getNexoItem("server_icon", "§bSort", lore);
    }

    private ItemStack filterButton(Player player) {
        ItemRarity filter = filterByPlayer.get(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(TooltipUtil.selectionLine(filter == null, "All"));
        for (ItemRarity rarity : ItemRarity.values()) {
            lore.add(TooltipUtil.selectionLine(filter == rarity, rarity.getColor() + rarity.name()));
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.getNexoItem("filter", "§bFilter Rarity", lore);
    }

    private ItemStack selectionInfo(Player player) {
        int selected = selectedEntryKeys.getOrDefault(player.getUniqueId(), new LinkedHashSet<>()).size();
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("Selected: " + selected + "/5", "Shift + Right-click to lock/unlock pets"));
        return GuiUtil.getNexoItem("info", "§eSelection Info", lore);
    }

    private ItemRarity nextFilter(ItemRarity current, boolean forward) {
        List<ItemRarity> order = new ArrayList<>();
        order.add(null);
        order.addAll(Arrays.asList(ItemRarity.values()));
        int idx = order.indexOf(current);
        idx = forward ? idx + 1 : idx - 1;
        if (idx >= order.size()) idx = 0;
        if (idx < 0) idx = order.size() - 1;
        return order.get(idx);
    }

    private void openConfirm(Player player) {
        List<String> selected = selectedPetIds(player);
        if (selected.isEmpty()) {
            PetChatUtil.send(player, "Select pets before merging.");
            return;
        }
        Inventory inv = GuiBuilder.create(CONFIRM_SIZE, confirmTitle).filler(Material.GRAY_STAINED_GLASS_PANE).build();
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList(
                "Selected pets: " + selected.size(),
                "Success chance: " + Math.min(100, selected.size() * 20) + "%"));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to confirm merge", null));

        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(11, ctx -> GuiUtil.getNexoItem("check", "§aConfirm Merge", lore),
                (click, context) -> handleMergeConfirm(player)));
        widgets.add(new ActionWidget(15, ctx -> GuiUtil.getNexoItem("cross", "§cCancel", TooltipUtil.clickInstructions("to go back", null)),
                (click, context) -> openMerge(player)));
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(inv, player, widgets);
        player.openInventory(inv);
        PetFeedbackUtil.playMenuOpen(player);
    }

    private void handleMergeConfirm(Player player) {
        List<String> selected = List.copyOf(selectedPetIds(player));
        if (selected.isEmpty()) {
            PetChatUtil.send(player, "Select pets before merging.");
            openMerge(player);
            return;
        }
        player.closeInventory();
        PetFeedbackUtil.runMergeAnimation(player, () -> {
            PetManager.MergeResult result = petManager.mergeSelectedPets(player, selected);
            if (!result.success()) {
                PetFeedbackUtil.playMergeResult(player, false);
                PetChatUtil.send(player, result.message());
                openMerge(player);
                return;
            }
            selectedEntryKeys.remove(player.getUniqueId());
            visibleEntries.remove(player.getUniqueId());
            entryIndexByPlayer.remove(player.getUniqueId());
            PetFeedbackUtil.playMergeResult(player, true);
            PetChatUtil.send(player, result.message());
            PetDefinition reward = petManager.getDefinition(result.rewardPetId()).orElse(null);
            if (reward != null) {
                Map<PetDefinition, Integer> gained = new HashMap<>();
                gained.put(reward, 1);
                PetPullSummaryUtil.sendSummary(player, "Gained", gained);
            }
            openMerge(player);
        });
    }

    private void mergeAllDuplicates(Player player) {
        PetManager.MergeAllResult result = petManager.mergeAllDuplicates(player);
        if (result.mergesCompleted() <= 0) {
            PetChatUtil.send(player, "No duplicates available to merge.");
        } else {
            PetChatUtil.send(player, "Merge all complete: " + result.mergesCompleted() + " merge(s).");
            PetPullSummaryUtil.sendSummary(player, "Gained", result.gainedPets());
        }
        selectedEntryKeys.remove(player.getUniqueId());
        visibleEntries.remove(player.getUniqueId());
        entryIndexByPlayer.remove(player.getUniqueId());
        PetFeedbackUtil.playMergeResult(player, result.mergesCompleted() > 0);
        openMerge(player);
    }

    private List<String> selectedPetIds(Player player) {
        UUID id = player.getUniqueId();
        LinkedHashSet<String> keys = selectedEntryKeys.getOrDefault(id, new LinkedHashSet<>());
        Map<String, CopyEntry> byKey = entryIndexByPlayer.getOrDefault(id, Map.of());
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            CopyEntry entry = byKey.get(key);
            if (entry != null) result.add(entry.petId());
        }
        return result;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String viewTitle = LEGACY.serialize(event.getView().title());
        if (!GuiUtil.titleMatches(viewTitle, mergeTitle)
                && !GuiUtil.titleMatches(viewTitle, selectTitle)
                && !GuiUtil.titleMatches(viewTitle, confirmTitle)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(me.nakilex.levelplugin.Main.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            String topTitle = LEGACY.serialize(player.getOpenInventory().title());
            if (GuiUtil.titleMatches(topTitle, mergeTitle)
                    || GuiUtil.titleMatches(topTitle, selectTitle)
                    || GuiUtil.titleMatches(topTitle, confirmTitle)) {
                return;
            }
            widgetsByPlayer.remove(player.getUniqueId());
            visibleEntries.remove(player.getUniqueId());
            entryIndexByPlayer.remove(player.getUniqueId());
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        widgetsByPlayer.remove(id);
        pageByPlayer.remove(id);
        filterByPlayer.remove(id);
        sortByPlayer.remove(id);
        selectedEntryKeys.remove(id);
        visibleEntries.remove(id);
        entryIndexByPlayer.remove(id);
    }

    private ItemRarity nextRarity(ItemRarity rarity) {
        if (rarity == null) {
            return ItemRarity.COMMON;
        }
        ItemRarity[] values = ItemRarity.values();
        int index = rarity.ordinal();
        return index >= values.length - 1 ? rarity : values[index + 1];
    }

    private ItemRarity selectedRarity(Player player, Set<String> selectedKeys) {
        if (selectedKeys == null || selectedKeys.isEmpty()) {
            return null;
        }
        Map<String, CopyEntry> byKey = entryIndexByPlayer.getOrDefault(player.getUniqueId(), Map.of());
        for (String key : selectedKeys) {
            CopyEntry copy = byKey.get(key);
            if (copy != null) {
                return copy.rarity();
            }
        }
        return null;
    }

    private void sanitizeSelected(Player player) {
        UUID id = player.getUniqueId();
        LinkedHashSet<String> selected = selectedEntryKeys.get(id);
        if (selected == null || selected.isEmpty()) {
            return;
        }
        Map<String, CopyEntry> byKey = entryIndexByPlayer.getOrDefault(id, Map.of());
        selected.removeIf(key -> !byKey.containsKey(key));
        ItemRarity rarity = selectedRarity(player, selected);
        if (rarity != null) {
            selected.removeIf(key -> {
                CopyEntry copy = byKey.get(key);
                return copy != null && copy.rarity() != rarity;
            });
        }
    }

    private ChatColor chanceColor(int chance) {
        if (chance <= 20) return ChatColor.RED;
        if (chance <= 40) return ChatColor.GOLD;
        if (chance <= 60) return ChatColor.YELLOW;
        if (chance <= 80) return ChatColor.GREEN;
        return ChatColor.AQUA;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String viewTitle = LEGACY.serialize(event.getView().title());
        if (!GuiUtil.titleMatches(viewTitle, mergeTitle)
                && !GuiUtil.titleMatches(viewTitle, selectTitle)
                && !GuiUtil.titleMatches(viewTitle, confirmTitle)) {
            return;
        }
        if (!handleWidgetClick(event, player)) event.setCancelled(true);
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return false;
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) return false;
        GuiWidget widget = widgets.stream().filter(w -> w.handlesSlot(slot)).findFirst().orElse(null);
        if (widget == null) return false;
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void render(Inventory inv, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inv);
        GuiContext context = new GuiContext(player, inv);
        for (GuiWidget widget : widgets) widget.contribute(layout, context);
    }

    private record CopyEntry(String entryKey, String petId, String displayName, ItemRarity rarity, int level, long acquiredAt) {}

    private enum SortMode {
        DATE_ACQUIRED("Date Acquired"),
        LEVEL("Level"),
        RARITY("Rarity");
        private final String label;
        SortMode(String label) { this.label = label; }
        private static SortMode next(SortMode current, boolean forward) {
            SortMode[] values = values();
            int i = current.ordinal() + (forward ? 1 : -1);
            if (i >= values.length) i = 0;
            if (i < 0) i = values.length - 1;
            return values[i];
        }
    }
}

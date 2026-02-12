package me.nakilex.levelplugin.pet.gui;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetManager.InvestResult;
import me.nakilex.levelplugin.pet.PetProgression;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetGuiUtil;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
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

public class PetGUI implements Listener {
    private static final int GUI_SIZE = 54;
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int CONFIRM_SIZE = 27;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private final PetManager petManager;
    private final PetSettingsGUI petSettingsGUI;
    private PetMergeGUI petMergeGUI;
    private final String titlePrefix = ChatUtil.applyEmojis("§8Pets equipped");
    private final String confirmTitle = ChatUtil.applyEmojis("§8Confirm Pet Action");
    private final Map<UUID, Integer> pages = new java.util.HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new java.util.HashMap<>();
    private final Map<UUID, ItemRarity> filterByPlayer = new java.util.HashMap<>();
    private final Map<UUID, SortMode> sortByPlayer = new java.util.HashMap<>();
    private final Map<UUID, List<GuiWidget>> confirmWidgetsByPlayer = new java.util.HashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new java.util.HashMap<>();

    public PetGUI(PetManager petManager, PetSettingsGUI petSettingsGUI) {
        this.petManager = petManager;
        this.petSettingsGUI = petSettingsGUI;
    }

    public void setPetMergeGUI(PetMergeGUI petMergeGUI) {
        this.petMergeGUI = petMergeGUI;
    }

    private String buildTitle(Player player) {
        int equipped = petManager.getEquippedPetCount(player.getUniqueId());
        int maxEquippable = petManager.getMaxEquippablePets(player.getUniqueId());
        return ChatUtil.applyEmojis("§8Pets equipped (§f" + equipped + "§8/§f" + maxEquippable + "§8)");
    }

    public void open(Player player, int page) {
        List<PetDefinition> defs = applySortAndFilter(player, petManager.getOwnedPets(player.getUniqueId()));
        int maxPage = Math.max(0, (defs.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), current);

        Inventory inv = GuiBuilder.create(GUI_SIZE, buildTitle(player))
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
        if (GuiUtil.titleStartsWith(viewTitle, titlePrefix)) {
            if (!handleWidgetClick(event, player)) {
                event.setCancelled(true);
            }
            return;
        }
        if (GuiUtil.titleMatches(viewTitle, confirmTitle)) {
            if (!handleConfirmClick(event, player)) {
                event.setCancelled(true);
            }
        }
    }

    private List<GuiWidget> buildPetWidgets(Player player, List<PetDefinition> defs, int page, int maxPage) {
        List<GuiWidget> widgets = new ArrayList<>();
        if (defs.isEmpty()) {
            widgets.add(new ActionWidget(22, ctx -> createEmptyItem(), (click, context) -> {}));
        }
        int start = page * PAGE_SIZE;
        int end = Math.min(defs.size(), start + PAGE_SIZE);
        var profile = petManager.getProfile(player.getUniqueId());
        String activeId = profile.activePetId();
        for (int i = start; i < end; i++) {
            PetDefinition def = defs.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            int xp = profile.getPetXp(def.id());
            int tier = profile.getPetTier(def.id());
            int copies = profile.getPetCopies(def.id());
            int level = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
            List<PetEffectDefinition> effects = def.effectsForLevel(level, tier);
            boolean equipped = activeId != null && activeId.equalsIgnoreCase(def.id());
            ItemStack icon = PetGuiUtil.createPetIcon(def, level, xp, tier, def.ownershipStats(), effects, copies, equipped);
            String petId = def.id();
            widgets.add(new ActionWidget(slot, ctx -> icon, (click, context) -> {
                if (click.isRightClick()) {
                    if (handleInvestOrSell(player, def, tier)) {
                        logClick(player, petId, "right-invest-sell", true);
                        return;
                    }
                } else if (click.isLeftClick()) {
                    boolean success = equipped ? petManager.dismissPet(player) : petManager.summonPet(player, petId);
                    logClick(player, petId, equipped ? "left-unequip" : "left-equip", success);
                }
                open(player, pages.getOrDefault(player.getUniqueId(), 0));
            }));
        }

        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT, ctx -> createNavItem(false),
                    (click, context) -> open(player, page - 1)));
        }
        if (page < maxPage) {
            widgets.add(new ActionWidget(NEXT_SLOT, ctx -> createNavItem(true),
                    (click, context) -> open(player, page + 1)));
        }
        if (petSettingsGUI != null) {
            widgets.add(new ActionWidget(49, ctx -> createSettingsItem(),
                    (click, context) -> petSettingsGUI.open(player)));
        }
        if (petMergeGUI != null) {
            widgets.add(new ActionWidget(48, ctx -> createMergeItem(player),
                    (click, context) -> petMergeGUI.openMerge(player)));
        }
        widgets.add(new ActionWidget(51, ctx -> sortButton(player), (click, context) -> {
            UUID id = player.getUniqueId();
            SortMode next = SortMode.next(sortByPlayer.getOrDefault(id, SortMode.NAME), click.isLeftClick());
            sortByPlayer.put(id, next);
            open(player, 0);
        }));
        widgets.add(new ActionWidget(52, ctx -> filterButton(player), (click, context) -> {
            UUID id = player.getUniqueId();
            filterByPlayer.put(id, nextFilter(filterByPlayer.get(id), click.isLeftClick()));
            open(player, 0);
        }));
        widgets.add(new ActionWidget(50, ctx -> createOwnershipSummaryItem(player), (click, context) -> {}));
        return widgets;
    }

    private ItemStack createNavItem(boolean next) {
        String name = next ? "§aNext Page" : "§aPrevious Page";
        String id = next ? "arrow_right" : "arrow_left";
        return GuiUtil.getNexoItem(id, name, TooltipUtil.clickInstructions("to change page", null));
    }

    private ItemStack createSettingsItem() {
        List<String> lore = TooltipUtil.clickInstructions("to open settings", null);
        return GuiUtil.createGuiItem(Material.COMPARATOR, "§bPet Settings", lore);
    }


    private ItemStack createMergeItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("Merge up to 5 pets into one", "Success chance scales with selected amount"));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to open merge menu", null));
        return GuiUtil.createGuiItem(Material.TURTLE_EGG, "§dPet Merge", lore);
    }


    private ItemStack sortButton(Player player) {
        SortMode mode = sortByPlayer.getOrDefault(player.getUniqueId(), SortMode.NAME);
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

    private ItemRarity nextFilter(ItemRarity current, boolean forward) {
        List<ItemRarity> order = new ArrayList<>();
        order.add(null);
        order.addAll(List.of(ItemRarity.values()));
        int idx = order.indexOf(current);
        idx = forward ? idx + 1 : idx - 1;
        if (idx >= order.size()) idx = 0;
        if (idx < 0) idx = order.size() - 1;
        return order.get(idx);
    }

    private List<PetDefinition> applySortAndFilter(Player player, List<PetDefinition> defs) {
        UUID id = player.getUniqueId();
        ItemRarity filter = filterByPlayer.get(id);
        SortMode sortMode = sortByPlayer.getOrDefault(id, SortMode.NAME);
        var profile = petManager.getProfile(id);
        List<PetDefinition> filtered = new ArrayList<>();
        for (PetDefinition def : defs) {
            if (filter != null && def.rarity() != filter) {
                continue;
            }
            filtered.add(def);
        }
        filtered.sort((a, b) -> switch (sortMode) {
            case NAME -> a.displayName().compareToIgnoreCase(b.displayName());
            case RARITY -> Integer.compare(b.rarity().ordinal(), a.rarity().ordinal());
            case LEVEL -> {
                int la = PetProgression.levelFromXp(profile.getPetXp(a.id()), a.xpPerLevel(), a.maxLevel());
                int lb = PetProgression.levelFromXp(profile.getPetXp(b.id()), b.xpPerLevel(), b.maxLevel());
                yield Integer.compare(lb, la);
            }
        });
        return filtered;
    }

    private ItemStack createOwnershipSummaryItem(Player player) {
        Map<StatType, Integer> totals = petManager.getTotalOwnedStatBonuses(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        if (totals.isEmpty()) {
            lore.addAll(TooltipUtil.bulletList("Unlock pets to gain ownership stat bonuses."));
        } else {
            lore.add("§7Total bonuses from unlocked pets:");
            lore.add(" ");
            for (StatType stat : StatType.DISPLAY_ORDER) {
                int value = totals.getOrDefault(stat, 0);
                if (value != 0) {
                    lore.add("§7• " + GuiUtil.formatStatLine(stat, value, false));
                }
            }
        }
        return GuiUtil.createGuiItem(Material.BOOK, "§aOwnership Bonuses", lore);
    }

    private ItemStack createEmptyItem() {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7No pets in your inventory yet.");
        lore.addAll(TooltipUtil.bulletList("Use /debug petpull to pull pets."));
        return GuiUtil.createGuiItem(Material.BARRIER, "§cNo Pets Found", lore);
    }

    private boolean handleInvestOrSell(Player player, PetDefinition def, int tier) {
        int investable = petManager.getInvestableCopies(player, def.id());
        if (tier >= petManager.getMaxTier()) {
            int sellable = petManager.getSellableCopies(player, def.id());
            if (sellable <= 0) {
                PetChatUtil.send(player, "No extra copies to sell.");
                return false;
            }
            openConfirm(player, new PendingAction(ActionType.SELL, def.id(), sellable));
            return true;
        }
        if (investable <= 0) {
            PetChatUtil.send(player, "Not enough copies to invest.");
            return false;
        }
        openConfirm(player, new PendingAction(ActionType.INVEST, def.id(), investable));
        return true;
    }

    private void openConfirm(Player player, PendingAction action) {
        Inventory inv = GuiBuilder.create(CONFIRM_SIZE, confirmTitle)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        ItemStack confirm = GuiUtil.getNexoItem("check", "§aConfirm");
        ItemMeta meta = confirm.getItemMeta();
        if (meta != null) {
            List<String> lore = buildConfirmLore(player, action);
            meta.setLore(lore);
            confirm.setItemMeta(meta);
        }
        List<GuiWidget> widgets = buildConfirmWidgets(player, action, confirm);
        confirmWidgetsByPlayer.put(player.getUniqueId(), widgets);
        pendingActions.put(player.getUniqueId(), action);
        renderWidgets(inv, player, widgets);
        player.openInventory(inv);
    }

    private List<String> buildConfirmLore(Player player, PendingAction action) {
        List<String> lore = new ArrayList<>();
        petManager.getDefinition(action.petId()).ifPresent(def -> {
            lore.add(" ");
            lore.add("§7Pet: §f" + def.displayName());
            if (action.type() == ActionType.INVEST) {
                int tier = petManager.getProfile(player.getUniqueId()).getPetTier(def.id());
                int maxTier = petManager.getMaxTier();
                int investable = petManager.getInvestableCopies(player, def.id());
                int investCount = Math.min(investable, Math.max(0, maxTier - tier));
                int newTier = tier + investCount;
                int copies = petManager.getProfile(player.getUniqueId()).getPetCopies(def.id());
                int remaining = Math.max(0, copies - 1 - investCount);
                List<String> bullets = new ArrayList<>();
                bullets.add("Increase tier from " + tier + " to " + newTier);
                bullets.add("Consumes " + investCount + " extra copies");
                if (newTier >= maxTier && remaining > 0) {
                    int coins = remaining * petManager.getSellValue(def.rarity());
                    bullets.add("Sell " + remaining + " extra copies");
                    bullets.add("Earn " + coins + " coins");
                }
                lore.addAll(TooltipUtil.bulletList(bullets.toArray(new String[0])));
            } else {
                int coins = action.amount() * petManager.getSellValue(def.rarity());
                lore.addAll(TooltipUtil.bulletList(
                        "Sell " + action.amount() + " extra copies",
                        "Earn " + coins + " coins"));
            }
        });
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to confirm", null));
        return lore;
    }

    private List<GuiWidget> buildConfirmWidgets(Player player, PendingAction action, ItemStack confirmItem) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(CONFIRM_YES_SLOT, ctx -> confirmItem,
                (click, context) -> handleConfirm(player, action)));
        widgets.add(new ActionWidget(CONFIRM_NO_SLOT,
                ctx -> GuiUtil.getNexoItem("cross", "§cCancel"),
                (click, context) -> open(player, pages.getOrDefault(player.getUniqueId(), 0))));
        return widgets;
    }

    private void handleConfirm(Player player, PendingAction action) {
        if (action.type() == ActionType.INVEST) {
            InvestResult result = petManager.investAllCopies(player, action.petId());
            if (result.investedCopies() > 0) {
                PetChatUtil.send(player, "Pet tier upgraded.");
            } else {
                PetChatUtil.send(player, "Unable to invest copies.");
            }
            if (result.coinsEarned() > 0) {
                CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.COINS, result.coinsEarned());
            }
        } else {
            int coins = petManager.sellPetCopies(player, action.petId(), action.amount());
            if (coins > 0) {
                CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.COINS, coins);
            } else {
                PetChatUtil.send(player, "No copies sold.");
            }
        }
        pendingActions.remove(player.getUniqueId());
        open(player, pages.getOrDefault(player.getUniqueId(), 0));
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
        List<PetDefinition> defs = applySortAndFilter(player, petManager.getOwnedPets(player.getUniqueId()));
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

    private boolean handleConfirmClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = confirmWidgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
        logConfirmClick(player, event.getRawSlot());
        return handleWidgetClick(event, player, widgets);
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player, List<GuiWidget> widgets) {
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

    // Pet clicks are handled via ActionWidgets in buildPetWidgets.


    private enum SortMode {
        NAME("Name"),
        RARITY("Rarity"),
        LEVEL("Level");
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

    private record PendingAction(ActionType type, String petId, int amount) {}

    private enum ActionType {
        INVEST,
        SELL
    }

    private void logClick(Player player, String petId, String action, boolean success) {
        var logger = me.nakilex.levelplugin.Main.getInstance().getLogger();
        String activeId = petManager.getProfile(player.getUniqueId()).activePetId();
        logger.info("[PetGUI] player=" + player.getName()
                + " action=" + action
                + " pet=" + petId
                + " success=" + success
                + " active=" + (activeId == null ? "none" : activeId));
    }

    private void logConfirmClick(Player player, int slot) {
        var action = pendingActions.get(player.getUniqueId());
        var logger = me.nakilex.levelplugin.Main.getInstance().getLogger();
        logger.info("[PetGUI] confirm-click player=" + player.getName()
                + " slot=" + slot
                + " action=" + (action == null ? "none" : action.type())
                + " pet=" + (action == null ? "none" : action.petId()));
    }
}

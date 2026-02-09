package me.nakilex.levelplugin.pet.gui;

import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetProgression;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
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
    private final String title = ChatUtil.applyEmojis("§8Pets");
    private final String confirmTitle = ChatUtil.applyEmojis("§8Confirm Pet Action");
    private final Map<UUID, Integer> pages = new java.util.HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new java.util.HashMap<>();
    private final Map<UUID, List<GuiWidget>> confirmWidgetsByPlayer = new java.util.HashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new java.util.HashMap<>();

    public PetGUI(PetManager petManager, PetSettingsGUI petSettingsGUI) {
        this.petManager = petManager;
        this.petSettingsGUI = petSettingsGUI;
    }

    public void open(Player player, int page) {
        List<PetDefinition> defs = petManager.getOwnedPets(player.getUniqueId());
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
        if (GuiUtil.titleMatches(viewTitle, title)) {
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
            Map<StatType, Integer> stats = def.statsForLevel(level, tier);
            List<PetEffectDefinition> effects = def.effectsForLevel(level, tier);
            boolean equipped = activeId != null && activeId.equalsIgnoreCase(def.id());
            ItemStack icon = PetGuiUtil.createPetIcon(def, level, xp, tier, stats, effects, copies, equipped);
            String petId = def.id();
            widgets.add(new ActionWidget(slot, ctx -> icon, (click, context) -> {
                if (click.isShiftClick() && click.isRightClick() && equipped) {
                    petManager.dismissPet(player);
                } else if (click.isRightClick()) {
                    handleInvestOrSell(player, def, tier);
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
        if (petSettingsGUI != null) {
            widgets.add(new ActionWidget(49, ctx -> createSettingsItem(),
                    (click, context) -> petSettingsGUI.open(player)));
        }
        return widgets;
    }

    private ItemStack createNavItem(String name) {
        List<String> lore = TooltipUtil.clickInstructions("to change page", null);
        return GuiUtil.createGuiItem(Material.ARROW, name, lore);
    }

    private ItemStack createSettingsItem() {
        List<String> lore = TooltipUtil.clickInstructions("to open settings", null);
        return GuiUtil.createGuiItem(Material.COMPARATOR, "§bPet Settings", lore);
    }

    private ItemStack createEmptyItem() {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7No pets in your inventory yet.");
        lore.addAll(TooltipUtil.bulletList("Use /debug petpull to pull pets."));
        return GuiUtil.createGuiItem(Material.BARRIER, "§cNo Pets Found", lore);
    }

    private void handleInvestOrSell(Player player, PetDefinition def, int tier) {
        int investable = petManager.getInvestableCopies(player, def.id());
        if (tier >= petManager.getMaxTier()) {
            int sellable = petManager.getSellableCopies(player, def.id());
            if (sellable <= 0) {
                PetChatUtil.send(player, "No extra copies to sell.");
                return;
            }
            openConfirm(player, new PendingAction(ActionType.SELL, def.id(), sellable));
            return;
        }
        if (investable <= 0) {
            PetChatUtil.send(player, "Not enough copies to invest.");
            return;
        }
        openConfirm(player, new PendingAction(ActionType.INVEST, def.id(), 1));
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
                lore.addAll(TooltipUtil.bulletList(
                        "Increase tier from " + tier + " to " + (tier + 1),
                        "Consumes 1 extra copy"));
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
            boolean invested = petManager.investTier(player, action.petId());
            PetChatUtil.send(player, invested ? "Pet tier upgraded." : "Unable to invest copies.");
        } else {
            int coins = petManager.sellPetCopies(player, action.petId(), action.amount());
            PetChatUtil.send(player, coins > 0 ? "Sold copies for " + coins + " coins." : "No copies sold.");
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
        List<PetDefinition> defs = petManager.getOwnedPets(player.getUniqueId());
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

    private record PendingAction(ActionType type, String petId, int amount) {}

    private enum ActionType {
        INVEST,
        SELL
    }
}

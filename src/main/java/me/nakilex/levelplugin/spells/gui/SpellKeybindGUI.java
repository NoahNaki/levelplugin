package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.input.SpellKeybindLayout;
import me.nakilex.levelplugin.spells.input.SpellKeybindManager;
import me.nakilex.levelplugin.spells.input.SpellKeybindSlot;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpellKeybindGUI implements Listener {
    private static final String TITLE = "Spell Keybinds";
    private static final int GUI_SIZE = 54;

    private static final int CLASS_SLOT = 4;
    private static final int MODE_SLOT = 13;
    private static final int[] KEYBIND_SLOTS = {21, 22, 23, 24};

    private static final int CANCEL_SLOT = 47;
    private static final int INFO_SLOT = 49;
    private static final int SAVE_SLOT = 51;

    private static final List<SpellInputType> BINDABLE_SPELLS = List.of(
            SpellInputType.SPELL_1,
            SpellInputType.SPELL_2,
            SpellInputType.SPELL_3,
            SpellInputType.SPELL_4
    );

    private final SettingsManager settingsManager;
    private final SettingsGUI settingsGUI;
    private final SpellKeybindManager keybindManager = SpellKeybindManager.getInstance();
    private final Map<UUID, ViewState> viewStates = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public SpellKeybindGUI(SettingsManager settingsManager, SettingsGUI settingsGUI) {
        this.settingsManager = settingsManager;
        this.settingsGUI = settingsGUI;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        ViewState state = viewStates.computeIfAbsent(player.getUniqueId(), id -> createState(player));
        Inventory gui = buildInventory(player, state);
        List<GuiWidget> widgets = buildWidgets(player, state);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(gui, player, widgets);
        player.openInventory(gui);
    }

    private ViewState createState(Player player) {
        ViewState state = new ViewState();
        state.viewClass = PlayerClassManager.getInstance().getPlayerClass(player);
        state.viewMode = settingsManager.getSettings(player).getSpellInputMode();
        return state;
    }

    private Inventory buildInventory(Player player, ViewState state) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        return gui;
    }

    private ItemStack createClassItem(Player player, ViewState state) {
        ItemStack item = GuiUtil.getNexoItem("info", ChatColor.AQUA + "Class Keybinds");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Viewing: " + ChatColor.WHITE + state.viewClass.getDisplayName());
            List<PlayerClass> unlocked = getUnlockedClasses(player);
            if (unlocked.size() > 1) {
                lore.add(" ");
                lore.addAll(TooltipUtil.clickInstructions("to view next class", "to view previous class"));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createModeItem(SpellInputMode mode) {
        ItemStack item = GuiUtil.getNexoItem("refresh", ChatColor.AQUA + "Keybind Mode");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Select which inputs to view.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(mode == SpellInputMode.MOUSE_COMBO,
                    ChatColor.WHITE + SpellInputMode.MOUSE_COMBO.getDisplayName()));
            lore.add(TooltipUtil.selectionLine(mode == SpellInputMode.MOUSE_AND_KEYBOARD,
                    ChatColor.WHITE + SpellInputMode.MOUSE_AND_KEYBOARD.getDisplayName()));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to toggle", null));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createKeybindItem(SpellInputMode mode, SpellKeybindSlot slot, SpellInputType bound,
                                        boolean archerFamily) {
        String sequence = mode == SpellInputMode.MOUSE_COMBO
                ? SpellKeybindLayout.comboSequenceForSlot(archerFamily, slot)
                : SpellKeybindLayout.keyboardSequenceForSlot(slot);
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + sequence);
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            String spellName = SpellKeybindLayout.spellDisplayName(bound);
            ChatColor spellColor = bound == null ? ChatColor.RED : ChatColor.GREEN;
            lore.add(ChatColor.GRAY + "Bound: " + spellColor + spellName);
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(SpellInputMode mode, EnumMap<SpellKeybindSlot, SpellInputType> bindings,
                                     boolean archerFamily) {
        List<String> issues = getValidationIssues(mode, bindings, archerFamily);
        if (issues.isEmpty()) {
            return null;
        }
        ItemStack item = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Keybind Issues");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Resolve the following:");
            lore.addAll(TooltipUtil.bulletList(issues.toArray(new String[0])));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> getValidationIssues(SpellInputMode mode, EnumMap<SpellKeybindSlot, SpellInputType> bindings,
                                             boolean archerFamily) {
        List<String> issues = new ArrayList<>();
        List<String> unbound = new ArrayList<>();
        Map<SpellInputType, Integer> counts = new EnumMap<>(SpellInputType.class);
        for (SpellKeybindSlot slot : SpellKeybindSlot.values()) {
            SpellInputType bound = bindings.get(slot);
            if (bound == null) {
                unbound.add(getKeybindLabel(mode, slot, archerFamily));
                continue;
            }
            counts.merge(bound, 1, Integer::sum);
        }
        if (!unbound.isEmpty()) {
            issues.add("Unbound keys: " + String.join(", ", unbound));
        }
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<SpellInputType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(SpellKeybindLayout.spellDisplayName(entry.getKey()));
            }
        }
        if (!duplicates.isEmpty()) {
            issues.add("Duplicate spells: " + String.join(", ", duplicates));
        }
        return issues;
    }

    private String getKeybindLabel(SpellInputMode mode, SpellKeybindSlot slot, boolean archerFamily) {
        return mode == SpellInputMode.MOUSE_COMBO
                ? SpellKeybindLayout.comboSequenceForSlot(archerFamily, slot)
                : SpellKeybindLayout.keyboardSequenceForSlot(slot);
    }

    private List<PlayerClass> getUnlockedClasses(Player player) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        Set<PlayerClass> unlocked = stats.unlockedClasses == null ? Set.of() : stats.unlockedClasses;
        List<PlayerClass> ordered = new ArrayList<>();
        for (PlayerClass playerClass : PlayerClass.values()) {
            if (unlocked.contains(playerClass)) {
                ordered.add(playerClass);
            }
        }
        if (ordered.isEmpty()) {
            ordered.add(PlayerClassManager.getInstance().getPlayerClass(player));
        }
        return ordered;
    }

    private Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> getProfile(UUID playerId,
                                                                                      PlayerClass playerClass,
                                                                                      ViewState state) {
        return state.bindings.computeIfAbsent(playerClass, cls -> loadProfile(playerId, cls));
    }

    private Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> loadProfile(UUID playerId,
                                                                                       PlayerClass playerClass) {
        Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> profile = new EnumMap<>(SpellInputMode.class);
        for (SpellInputMode mode : SpellInputMode.values()) {
            profile.put(mode, keybindManager.getBindings(playerId, playerClass, mode));
        }
        return profile;
    }

    private SpellInputType cycleBinding(SpellInputType current, boolean forward) {
        List<SpellInputType> options = new ArrayList<>();
        options.add(null);
        options.addAll(BINDABLE_SPELLS);
        int idx = options.indexOf(current);
        if (idx < 0) {
            idx = 0;
        }
        idx = forward ? idx + 1 : idx - 1;
        if (idx < 0) {
            idx = options.size() - 1;
        } else if (idx >= options.size()) {
            idx = 0;
        }
        return options.get(idx);
    }

    private void saveBindings(Player player, ViewState state) {
        UUID playerId = player.getUniqueId();
        for (Map.Entry<PlayerClass, Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> entry
                : state.bindings.entrySet()) {
            PlayerClass playerClass = entry.getKey();
            Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> profile = entry.getValue();
            for (Map.Entry<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> modeEntry : profile.entrySet()) {
                keybindManager.setBindings(playerId, playerClass, modeEntry.getKey(), modeEntry.getValue());
            }
        }
        Integer activeSlot = me.nakilex.levelplugin.player.profile.ProfileManager.getInstance()
                .getActiveSlot(playerId);
        if (activeSlot != null && activeSlot >= 0) {
            Main.getInstance().getPlayerConfig().setProfileSpellKeybinds(playerId, activeSlot,
                    keybindManager.getAllBindings(playerId));
            Main.getInstance().getPlayerConfig().saveConfigFile();
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Spell keybinds saved.");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            event.setCancelled(true);
            return;
        }
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        viewStates.remove(event.getPlayer().getUniqueId());
        widgetsByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private void refresh(Player player, ViewState state) {
        Inventory gui = buildInventory(player, state);
        List<GuiWidget> widgets = buildWidgets(player, state);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(gui, player, widgets);
        Inventory current = player.getOpenInventory().getTopInventory();
        if (GuiUtil.titleMatches(player.getOpenInventory().getTitle(), TITLE)
                && current.getSize() == gui.getSize()) {
            current.setContents(gui.getContents());
        } else {
            player.openInventory(gui);
        }
    }

    private List<GuiWidget> buildWidgets(Player player, ViewState state) {
        List<GuiWidget> widgets = new ArrayList<>();
        Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> profile =
                getProfile(player.getUniqueId(), state.viewClass, state);
        EnumMap<SpellKeybindSlot, SpellInputType> bindings = profile.get(state.viewMode);
        boolean archerFamily = ClassUtil.isArcherFamily(state.viewClass);

        widgets.add(new ActionWidget(CLASS_SLOT,
                context -> createClassItem(player, state),
                (click, context) -> handleClassClick(context.player(), state, click.isRightClick())));
        widgets.add(new ActionWidget(MODE_SLOT,
                context -> createModeItem(state.viewMode),
                (click, context) -> {
                    state.viewMode = state.viewMode.next();
                    refresh(context.player(), state);
                }));
        widgets.add(new ActionWidget(CANCEL_SLOT,
                context -> GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"),
                (click, context) -> {
                    viewStates.remove(context.player().getUniqueId());
                    settingsGUI.openSettingsMenu(context.player());
                }));
        widgets.add(new ActionWidget(SAVE_SLOT,
                context -> GuiUtil.getNexoItem("save", ChatColor.GREEN + "Save"),
                (click, context) -> {
                    saveBindings(context.player(), state);
                    viewStates.remove(context.player().getUniqueId());
                    settingsGUI.openSettingsMenu(context.player());
                }));
        widgets.add(new ActionWidget(INFO_SLOT,
                context -> createInfoItem(state.viewMode, bindings, archerFamily),
                null));

        for (int i = 0; i < KEYBIND_SLOTS.length; i++) {
            SpellKeybindSlot slot = SpellKeybindSlot.values()[i];
            SpellInputType bound = bindings.get(slot);
            int slotIndex = KEYBIND_SLOTS[i];
            widgets.add(new ActionWidget(slotIndex,
                    context -> createKeybindItem(state.viewMode, slot, bound, archerFamily),
                    (click, context) -> handleKeybindClick(context.player(), state, slot, click.isLeftClick())));
        }
        return widgets;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
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

    private void handleClassClick(Player player, ViewState state, boolean reverse) {
        List<PlayerClass> classes = getUnlockedClasses(player);
        if (classes.size() > 1) {
            int direction = reverse ? -1 : 1;
            int idx = classes.indexOf(state.viewClass);
            if (idx < 0) {
                idx = 0;
            }
            idx = (idx + direction + classes.size()) % classes.size();
            state.viewClass = classes.get(idx);
        }
        refresh(player, state);
    }

    private void handleKeybindClick(Player player, ViewState state, SpellKeybindSlot keybindSlot, boolean forward) {
        Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>> profile =
                getProfile(player.getUniqueId(), state.viewClass, state);
        EnumMap<SpellKeybindSlot, SpellInputType> bindings = profile.get(state.viewMode);
        SpellInputType current = bindings.get(keybindSlot);
        SpellInputType next = cycleBinding(current, forward);
        if (next == null) {
            bindings.remove(keybindSlot);
        } else {
            bindings.put(keybindSlot, next);
        }
        refresh(player, state);
    }

    private static final class ViewState {
        private PlayerClass viewClass;
        private SpellInputMode viewMode;
        private final Map<PlayerClass, Map<SpellInputMode, EnumMap<SpellKeybindSlot, SpellInputType>>> bindings =
                new EnumMap<>(PlayerClass.class);
    }
}

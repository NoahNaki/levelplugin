package me.nakilex.levelplugin.settings.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.spells.gui.SpellKeybindGUI;
import me.nakilex.levelplugin.spells.gui.SpellUpgradeGUI;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SettingsGUI implements Listener {

    private enum Filter { ALL, SOCIAL, VISUAL, COMBAT }
    private enum SortMode { DEFAULT, A_TO_Z }

    private record SettingEntry(String key, java.util.function.Function<GuiContext, ItemStack> icon,
                                java.util.function.BiConsumer<org.bukkit.event.inventory.ClickType, GuiContext> clickHandler) {}

    private static final int GUI_SIZE = 54;
    private static final int FILTER_SLOT = 48;
    private static final int SORT_SLOT = 50;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final SettingsManager settingsManager;
    private final Map<UUID, Filter> filters = new HashMap<>();
    private final Map<UUID, SortMode> sorts = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private SpellKeybindGUI spellKeybindGUI;
    private SpellUpgradeGUI spellUpgradeGUI;
    private PersonalEnvironmentSettingsGUI personalEnvironmentSettingsGUI;

    public SettingsGUI(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public void setSpellKeybindGUI(SpellKeybindGUI spellKeybindGUI) {
        this.spellKeybindGUI = spellKeybindGUI;
    }

    public void setSpellUpgradeGUI(SpellUpgradeGUI spellUpgradeGUI) {
        this.spellUpgradeGUI = spellUpgradeGUI;
    }

    public void setPersonalEnvironmentSettingsGUI(PersonalEnvironmentSettingsGUI personalEnvironmentSettingsGUI) {
        this.personalEnvironmentSettingsGUI = personalEnvironmentSettingsGUI;
    }

    public void openSettingsMenu(Player player) {
        PlayerSettings playerSettings = settingsManager.getSettings(player);
        Filter filter = filters.getOrDefault(player.getUniqueId(), Filter.ALL);

        Inventory gui = GuiBuilder.create(GUI_SIZE, "Settings")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildWidgets(player, playerSettings, filter, isOfficeErrandsLocked(player));
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(gui, player, widgets);

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        return GuiUtil.createGuiItem(mat, name, Arrays.asList(loreLines));
    }

    private ItemStack createVisibilityItem(PlayerVisibility vis, boolean locked) {
        Material mat;
        String status;
        switch (vis) {
            case SHOW_ALL -> { mat = Material.LIME_DYE; status = "All"; }
            case FRIENDS_ONLY -> { mat = Material.GREEN_DYE; status = "Friends"; }
            case HIDE_ALL -> { mat = Material.GRAY_DYE; status = "None"; }
            default -> { mat = Material.GRAY_DYE; status = "Unknown"; }
        }
        if (locked) {
            return createItem(mat, "§bPlayer Visibility",
                    "",
                    "§7Status: §f" + status,
                    "§cLocked during Office Errands",
                    "",
                    "§7Complete the quest to change.");
        }
        return createItem(mat, "§bPlayer Visibility",
                "",
                "§7Status: §f" + status,
                "",
                "§eClick to cycle");
    }



    private ItemStack createFilterItem(Filter filter) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Filter settings by category");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(filter == Filter.ALL, ChatColor.WHITE + "Show All"));
            lore.add(TooltipUtil.selectionLine(filter == Filter.SOCIAL, ChatColor.WHITE + "Social"));
            lore.add(TooltipUtil.selectionLine(filter == Filter.VISUAL, ChatColor.WHITE + "Visual"));
            lore.add(TooltipUtil.selectionLine(filter == Filter.COMBAT, ChatColor.WHITE + "Combat"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSortItem(SortMode sortMode) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sorting");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Sort the settings list.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(sortMode == SortMode.DEFAULT, ChatColor.WHITE + "Default"));
            lore.add(TooltipUtil.selectionLine(sortMode == SortMode.A_TO_Z, ChatColor.WHITE + "A → Z"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLootPickupFilterItem(PlayerSettings settings) {
        ItemRarity rarity = settings.getLootPickupRarity();
        ItemStack it = GuiUtil.getRarityArrowItem(rarity, ChatColor.AQUA + "Loot Pickup Filter");
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Pick up armor & weapons");
            lore.add(ChatColor.DARK_GRAY + "of this rarity or higher.");
            lore.add(" ");
            ItemRarity[] rarities = settings.getLootPickupRarities();
            for (ItemRarity entry : rarities) {
                String label = entry.getColor() + formatRarityLabel(entry);
                lore.add(TooltipUtil.selectionLine(entry == rarity, label));
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createSpellInputModeItem(PlayerSettings settings) {
        SpellInputMode mode = settings.getSpellInputMode();
        ItemStack item = GuiUtil.getNexoItem("info", ChatColor.AQUA + "Spell Input Mode");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Choose how spells are cast.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(mode == SpellInputMode.MOUSE_COMBO,
                    ChatColor.WHITE + "Mouse Combo Clicks"));
            lore.add(TooltipUtil.selectionLine(mode == SpellInputMode.MOUSE_AND_KEYBOARD,
                    ChatColor.WHITE + "Mouse + Keyboard"));
            lore.add(" ");
            lore.addAll(TooltipUtil.bulletList(
                    "Combo: R/L sequences (RRL, RLR, RRR, RLL)",
                    "Keyboard: Sneak + Click or Sneak + Sneak"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to cycle", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpellKeybindsItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Spell Keybinds");
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "View and adjust spell keybinds.");
            lore.addAll(TooltipUtil.bulletList(
                    "Keybinds are saved per class.",
                    "Use save/cancel to confirm changes."));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to open", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpellUpgradesItem() {
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Spells",
                List.of(" ", ChatColor.GRAY + "View your class spells and scaling.", " ",
                        ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to open"));
    }

    private String formatRarityLabel(ItemRarity rarity) {
        String name = rarity.name().toLowerCase(Locale.ROOT);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private boolean isOfficeErrandsLocked(Player player) {
        me.nakilex.levelplugin.Main main = me.nakilex.levelplugin.Main.getInstance();
        if (main == null || main.getQuestManager() == null) {
            return false;
        }
        return main.getQuestManager().getProgress(player.getUniqueId(),
                me.nakilex.levelplugin.quests.def.OfficeErrandsQuest.ID) != null
                && !main.getQuestManager().hasCompleted(player.getUniqueId(),
                me.nakilex.levelplugin.quests.def.OfficeErrandsQuest.ID);
    }

    @EventHandler
    public void onSettingsClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("Settings")) return;

        Player player = (Player) event.getWhoClicked();
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets(Player player, PlayerSettings settings, Filter filter, boolean lockedVisibility) {
        List<GuiWidget> widgets = new ArrayList<>();
        SortMode sortMode = sorts.getOrDefault(player.getUniqueId(), SortMode.DEFAULT);
        widgets.add(new ActionWidget(FILTER_SLOT,
                context -> createFilterItem(filter),
                (click, context) -> cycleFilter(context.player(), !click.isRightClick())));
        widgets.add(new ActionWidget(SORT_SLOT,
                context -> createSortItem(sortMode),
                (click, context) -> cycleSort(context.player(), !click.isRightClick())));

        List<SettingEntry> entries = new ArrayList<>();

        if (filter == Filter.ALL || filter == Filter.COMBAT) {
            entries.add(new SettingEntry("Damage Chat",
                    context -> GuiUtil.createToggleItem(settings.isDmgChatEnabled(), "§bDamage Chat", "§eClick to toggle"),
                    (click, context) -> toggleDamageChat(context.player(), settings)));
            entries.add(new SettingEntry("Damage Numbers",
                    context -> GuiUtil.createToggleItem(settings.isDmgNumberEnabled(), "§bDamage Numbers",
                            "§eClick to toggle and run /dmgnumber"),
                    (click, context) -> toggleDamageNumbers(context.player(), settings)));
            entries.add(new SettingEntry("Loot Pickup Filter",
                    context -> createLootPickupFilterItem(settings),
                    (click, context) -> cycleLootFilter(context.player(), settings, click.isLeftClick())));
            entries.add(new SettingEntry("Spell Input Mode",
                    context -> createSpellInputModeItem(settings),
                    (click, context) -> cycleSpellInputMode(context.player(), settings)));
            entries.add(new SettingEntry("Spell Keybinds",
                    context -> createSpellKeybindsItem(),
                    (click, context) -> {
                        if (spellKeybindGUI != null) {
                            spellKeybindGUI.open(context.player());
                        }
                    }));
            entries.add(new SettingEntry("Spells",
                    context -> createSpellUpgradesItem(),
                    (click, context) -> {
                        if (spellUpgradeGUI != null) {
                            spellUpgradeGUI.open(context.player());
                        }
                    }));
        }

        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            entries.add(new SettingEntry("Drop Details",
                    context -> GuiUtil.createToggleItem(settings.isDropDetailsEnabled(), "§bDrop Details",
                            "§eClick to toggle and run /toggle dropdetails"),
                    (click, context) -> toggleDropDetails(context.player(), settings)));
            entries.add(new SettingEntry("Drop Details Chat",
                    context -> GuiUtil.createToggleItem(settings.isDropDetailsChatEnabled(), "§bDrop Details Chat",
                            "§eClick to toggle and run /toggle dropdetailschat"),
                    (click, context) -> toggleDropDetailsChat(context.player(), settings)));
            entries.add(new SettingEntry("Auto Skip Cutscenes",
                    context -> GuiUtil.createToggleItem(settings.isAutoSkipCutscenes(), "§bAuto Skip Cutscenes",
                            "§eClick to toggle"),
                    (click, context) -> toggleAutoSkipCutscenes(context.player(), settings)));
            entries.add(new SettingEntry("Auto Skip Songs",
                    context -> GuiUtil.createToggleItem(settings.isAutoSkipSongs(), "§bAuto Skip Songs",
                            "§eClick to toggle and run /toggle songskip"),
                    (click, context) -> toggleAutoSkipSongs(context.player(), settings)));
            entries.add(new SettingEntry("Skill Point Reminder",
                    context -> GuiUtil.createToggleItem(settings.isSkillPointReminderEnabled(), "§bSkill Point Reminder",
                            "§eClick to toggle"),
                    (click, context) -> toggleSkillPointReminder(context.player(), settings)));
            entries.add(new SettingEntry("Full Inventory Title",
                    context -> GuiUtil.createToggleItem(settings.isFullInventoryTitleEnabled(), "§bFull Inventory Title",
                            "§eClick to toggle"),
                    (click, context) -> toggleFullInventoryTitle(context.player(), settings)));
            entries.add(new SettingEntry("Tips",
                    context -> GuiUtil.createToggleItem(settings.isTipsEnabled(), "§bTips",
                            "§eClick to toggle"),
                    (click, context) -> toggleTips(context.player(), settings)));
            entries.add(new SettingEntry("Booster Boss Bar",
                    context -> GuiUtil.createToggleItem(settings.isBoosterBossBarEnabled(), "§bBooster Boss Bar",
                            "§eClick to toggle"),
                    (click, context) -> toggleBoosterBossBar(context.player(), settings)));
            entries.add(new SettingEntry("Quest Path Particles",
                    context -> GuiUtil.createToggleItem(settings.isQuestTrackingParticlesEnabled(), "§bQuest Path Particles",
                            "§eClick to toggle"),
                    (click, context) -> toggleQuestTrackingParticles(context.player(), settings)));
            entries.add(new SettingEntry("Personal Environment",
                    context -> createPersonalEnvironmentItem(),
                    (click, context) -> openPersonalEnvironmentSettings(context.player())));
        }

        if (filter == Filter.ALL || filter == Filter.SOCIAL) {
            entries.add(new SettingEntry("Party Glow",
                    context -> GuiUtil.createToggleItem(settings.isPartyGlowEnabled(), "§bParty Glow",
                            "§eClick to toggle and run /partyglow"),
                    (click, context) -> togglePartyGlow(context.player(), settings)));
            entries.add(new SettingEntry("Friend Glow",
                    context -> GuiUtil.createToggleItem(settings.isFriendGlowEnabled(), "§bFriend Glow",
                            "§eClick to toggle and run /friendglow"),
                    (click, context) -> toggleFriendGlow(context.player(), settings)));
            entries.add(new SettingEntry("Public Balance",
                    context -> GuiUtil.createToggleItem(settings.isBalancePublic(), "§ePublic Balance",
                            "§eClick to toggle and run /toggle balancepublic"),
                    (click, context) -> toggleBalancePublic(context.player(), settings)));
            entries.add(new SettingEntry("Player Visibility",
                    context -> createVisibilityItem(settings.getPlayerVisibility(), lockedVisibility),
                    (click, context) -> toggleVisibility(context.player(), settings, lockedVisibility)));
            entries.add(new SettingEntry("Chat Games",
                    context -> GuiUtil.createToggleItem(settings.isChatGamesEnabled(), "§bChat Games",
                            "§eClick to toggle"),
                    (click, context) -> toggleChatGames(context.player(), settings)));
        }

        if (sortMode == SortMode.A_TO_Z) {
            entries.sort(Comparator.comparing(entry -> ChatColor.stripColor(entry.key()), String.CASE_INSENSITIVE_ORDER));
        }

        int count = Math.min(entries.size(), CONTENT_SLOTS.length);
        for (int i = 0; i < count; i++) {
            SettingEntry entry = entries.get(i);
            int slot = CONTENT_SLOTS[i];
            widgets.add(new ActionWidget(slot,
                    entry.icon,
                    (click, context) -> entry.clickHandler.accept(click, context)));
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

    private void cycleFilter(Player player, boolean forward) {
        Filter[] values = Filter.values();
        Filter current = filters.getOrDefault(player.getUniqueId(), Filter.ALL);
        int currentIndex = current.ordinal();
        int nextIndex = forward
                ? (currentIndex + 1) % values.length
                : (currentIndex - 1 + values.length) % values.length;
        filters.put(player.getUniqueId(), values[nextIndex]);
        openSettingsMenu(player);
    }

    private void cycleSort(Player player, boolean forward) {
        SortMode[] values = SortMode.values();
        SortMode current = sorts.getOrDefault(player.getUniqueId(), SortMode.DEFAULT);
        int currentIndex = current.ordinal();
        int nextIndex = forward
                ? (currentIndex + 1) % values.length
                : (currentIndex - 1 + values.length) % values.length;
        sorts.put(player.getUniqueId(), values[nextIndex]);
        openSettingsMenu(player);
    }

    private void toggleDamageChat(Player player, PlayerSettings settings) {
        settings.toggleDmgChat();
        boolean enabled = settings.isDmgChatEnabled();
        ChatToggleManager.getInstance().setEnabled(player, enabled);
        ToggleFeedbackUtil.sendToggle(player, "Damage chat", enabled);
        openSettingsMenu(player);
    }

    private void toggleDamageNumbers(Player player, PlayerSettings settings) {
        settings.toggleDmgNumber();
        Bukkit.dispatchCommand(player, "dmgnumber");
        openSettingsMenu(player);
    }

    private void toggleDropDetails(Player player, PlayerSettings settings) {
        Bukkit.dispatchCommand(player, "toggle dropdetails");
        openSettingsMenu(player);
    }

    private void toggleDropDetailsChat(Player player, PlayerSettings settings) {
        Bukkit.dispatchCommand(player, "toggle dropdetailschat");
        openSettingsMenu(player);
    }

    private void togglePartyGlow(Player player, PlayerSettings settings) {
        settings.togglePartyGlow();
        Bukkit.dispatchCommand(player, "partyglow");
        openSettingsMenu(player);
    }

    private void toggleFriendGlow(Player player, PlayerSettings settings) {
        settings.toggleFriendGlow();
        Bukkit.dispatchCommand(player, "friendglow");
        openSettingsMenu(player);
    }

    private void toggleBalancePublic(Player player, PlayerSettings settings) {
        settings.toggleBalancePublic();
        me.nakilex.levelplugin.Main main = me.nakilex.levelplugin.Main.getInstance();
        if (main != null && main.getLeaderboardManager() != null) {
        }
        openSettingsMenu(player);
    }

    private void toggleVisibility(Player player, PlayerSettings settings, boolean locked) {
        if (locked) {
            me.nakilex.levelplugin.utils.ChatMessageUtil.send(player,
                    me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.ERROR,
                    "Player visibility is locked during Office Errands.");
            openSettingsMenu(player);
            return;
        }
        settings.cyclePlayerVisibility();
        me.nakilex.levelplugin.Main.getInstance()
                .getPlayerVisibilityManager().updatePlayer(player);
        openSettingsMenu(player);
    }

    private void toggleAutoSkipCutscenes(Player player, PlayerSettings settings) {
        settings.toggleAutoSkipCutscenes();
        openSettingsMenu(player);
    }

    private void toggleAutoSkipSongs(Player player, PlayerSettings settings) {
        Bukkit.dispatchCommand(player, "toggle songskip");
        openSettingsMenu(player);
    }

    private void toggleSkillPointReminder(Player player, PlayerSettings settings) {
        settings.toggleSkillPointReminder();
        openSettingsMenu(player);
    }

    private void toggleFullInventoryTitle(Player player, PlayerSettings settings) {
        settings.toggleFullInventoryTitle();
        openSettingsMenu(player);
    }

    private void toggleTips(Player player, PlayerSettings settings) {
        settings.toggleTipsEnabled();
        ToggleFeedbackUtil.sendToggle(player, "Tips", settings.isTipsEnabled());
        openSettingsMenu(player);
    }

    private void toggleChatGames(Player player, PlayerSettings settings) {
        settings.toggleChatGamesEnabled();
        ToggleFeedbackUtil.sendToggle(player, "Chat games", settings.isChatGamesEnabled());
        openSettingsMenu(player);
    }

    private void toggleBoosterBossBar(Player player, PlayerSettings settings) {
        settings.toggleBoosterBossBar();
        var boosterManager = Main.getInstance().getBoosterManager();
        if (boosterManager != null) {
            boosterManager.refreshBossBar(player);
        }
        openSettingsMenu(player);
    }

    private void toggleQuestTrackingParticles(Player player, PlayerSettings settings) {
        settings.toggleQuestTrackingParticles();
        openSettingsMenu(player);
    }

    private void cycleLootFilter(Player player, PlayerSettings settings, boolean forward) {
        settings.cycleLootPickupRarity(forward);
        openSettingsMenu(player);
    }

    private void cycleSpellInputMode(Player player, PlayerSettings settings) {
        settings.cycleSpellInputMode();
        settingsManager.saveActiveProfileSettings(player);
        me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance().sync(player);
        openSettingsMenu(player);
    }

    private ItemStack createPersonalEnvironmentItem() {
        ItemStack item = GuiUtil.getNexoItem("placeholder_environment", ChatColor.AQUA + "Personal Environment");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Configure client-side");
            lore.add(ChatColor.GRAY + "weather and time.");
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to open", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openPersonalEnvironmentSettings(Player player) {
        if (personalEnvironmentSettingsGUI != null) {
            personalEnvironmentSettingsGUI.open(player);
        }
    }
}

package me.nakilex.levelplugin.settings.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.leaderboards.LeaderboardType;
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

    private static final int GUI_SIZE = 45;
    private static final int FILTER_SLOT = 36;
    private static final int LOOT_FILTER_SLOT = 32;
    private static final int CHAT_GAMES_SLOT = 27;
    private static final int SPELL_INPUT_SLOT = 33;
    private static final int SPELL_KEYBINDS_SLOT = 34;
    private static final int SPELL_UPGRADES_SLOT = 35;

    private final SettingsManager settingsManager;
    private final Map<UUID, Filter> filters = new HashMap<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private SpellKeybindGUI spellKeybindGUI;
    private SpellUpgradeGUI spellUpgradeGUI;

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

    public void openSettingsMenu(Player player) {
        PlayerSettings playerSettings = settingsManager.getSettings(player);
        Filter filter = filters.getOrDefault(player.getUniqueId(), Filter.ALL);

        Inventory gui = GuiBuilder.create(GUI_SIZE, "Settings")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
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
        String name = switch (filter) {
            case ALL -> "§bFilter: All";
            case SOCIAL -> "§bFilter: Social";
            case VISUAL -> "§bFilter: Visual";
            case COMBAT -> "§bFilter: Combat";
        };
        ItemStack it = GuiUtil.getNexoItem("refresh", name);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setLore(Collections.singletonList("§7Click to change filter"));
            it.setItemMeta(meta);
        }
        return it;
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
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Spell Upgrades",
                List.of(" ", ChatColor.GRAY + "Spend spell points to evolve spells.", " ",
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
        widgets.add(new ActionWidget(0,
                context -> GuiUtil.getNexoItem("arrow_left2", "§7Back"),
                (click, context) -> GuiUtil.openPlayerInventory(context.player())));
        widgets.add(new ActionWidget(FILTER_SLOT,
                context -> createFilterItem(filter),
                (click, context) -> cycleFilter(context.player())));

        if (filter == Filter.ALL || filter == Filter.COMBAT) {
            widgets.add(new ActionWidget(10,
                    context -> GuiUtil.createToggleItem(settings.isDmgChatEnabled(), "§bDamage Chat", "§eClick to toggle"),
                    (click, context) -> toggleDamageChat(context.player(), settings)));
            widgets.add(new ActionWidget(11,
                    context -> GuiUtil.createToggleItem(settings.isDmgNumberEnabled(), "§bDamage Numbers",
                            "§eClick to toggle and run /dmgnumber"),
                    (click, context) -> toggleDamageNumbers(context.player(), settings)));
            widgets.add(new ActionWidget(LOOT_FILTER_SLOT,
                    context -> createLootPickupFilterItem(settings),
                    (click, context) -> cycleLootFilter(context.player(), settings, click.isLeftClick())));
            widgets.add(new ActionWidget(SPELL_INPUT_SLOT,
                    context -> createSpellInputModeItem(settings),
                    (click, context) -> cycleSpellInputMode(context.player(), settings)));
            widgets.add(new ActionWidget(SPELL_KEYBINDS_SLOT,
                    context -> createSpellKeybindsItem(),
                    (click, context) -> {
                        if (spellKeybindGUI != null) {
                            spellKeybindGUI.open(context.player());
                        }
                    }));
            widgets.add(new ActionWidget(SPELL_UPGRADES_SLOT,
                    context -> createSpellUpgradesItem(),
                    (click, context) -> {
                        if (spellUpgradeGUI != null) {
                            spellUpgradeGUI.open(context.player());
                        }
                    }));
        }

        if (filter == Filter.ALL || filter == Filter.VISUAL) {
            widgets.add(new ActionWidget(12,
                    context -> GuiUtil.createToggleItem(settings.isDropDetailsEnabled(), "§bDrop Details",
                            "§eClick to toggle and run /toggle dropdetails"),
                    (click, context) -> toggleDropDetails(context.player(), settings)));
            widgets.add(new ActionWidget(13,
                    context -> GuiUtil.createToggleItem(settings.isDropDetailsChatEnabled(), "§bDrop Details Chat",
                            "§eClick to toggle and run /toggle dropdetailschat"),
                    (click, context) -> toggleDropDetailsChat(context.player(), settings)));
            widgets.add(new ActionWidget(22,
                    context -> GuiUtil.createToggleItem(settings.isAutoSkipCutscenes(), "§bAuto Skip Cutscenes",
                            "§eClick to toggle"),
                    (click, context) -> toggleAutoSkipCutscenes(context.player(), settings)));
            widgets.add(new ActionWidget(23,
                    context -> GuiUtil.createToggleItem(settings.isAutoSkipSongs(), "§bAuto Skip Songs",
                            "§eClick to toggle and run /toggle songskip"),
                    (click, context) -> toggleAutoSkipSongs(context.player(), settings)));
            widgets.add(new ActionWidget(24,
                    context -> GuiUtil.createToggleItem(settings.isSkillPointReminderEnabled(), "§bSkill Point Reminder",
                            "§eClick to toggle"),
                    (click, context) -> toggleSkillPointReminder(context.player(), settings)));
            widgets.add(new ActionWidget(25,
                    context -> GuiUtil.createToggleItem(settings.isFullInventoryTitleEnabled(), "§bFull Inventory Title",
                            "§eClick to toggle"),
                    (click, context) -> toggleFullInventoryTitle(context.player(), settings)));
            widgets.add(new ActionWidget(26,
                    context -> GuiUtil.createToggleItem(settings.isTipsEnabled(), "§bTips",
                            "§eClick to toggle"),
                    (click, context) -> toggleTips(context.player(), settings)));
            widgets.add(new ActionWidget(31,
                    context -> GuiUtil.createToggleItem(settings.isBoosterBossBarEnabled(), "§bBooster Boss Bar",
                            "§eClick to toggle"),
                    (click, context) -> toggleBoosterBossBar(context.player(), settings)));
            widgets.add(new ActionWidget(30,
                    context -> GuiUtil.createToggleItem(settings.isQuestTrackingParticlesEnabled(), "§bQuest Path Particles",
                            "§eClick to toggle"),
                    (click, context) -> toggleQuestTrackingParticles(context.player(), settings)));
        }

        if (filter == Filter.ALL || filter == Filter.SOCIAL) {
            widgets.add(new ActionWidget(14,
                    context -> GuiUtil.createToggleItem(settings.isPartyGlowEnabled(), "§bParty Glow",
                            "§eClick to toggle and run /partyglow"),
                    (click, context) -> togglePartyGlow(context.player(), settings)));
            widgets.add(new ActionWidget(15,
                    context -> GuiUtil.createToggleItem(settings.isFriendGlowEnabled(), "§bFriend Glow",
                            "§eClick to toggle and run /friendglow"),
                    (click, context) -> toggleFriendGlow(context.player(), settings)));
            widgets.add(new ActionWidget(16,
                    context -> GuiUtil.createToggleItem(settings.isBalancePublic(), "§ePublic Balance",
                            "§eClick to toggle and run /toggle balancepublic"),
                    (click, context) -> toggleBalancePublic(context.player(), settings)));
            widgets.add(new ActionWidget(21,
                    context -> createVisibilityItem(settings.getPlayerVisibility(), lockedVisibility),
                    (click, context) -> toggleVisibility(context.player(), settings, lockedVisibility)));
            widgets.add(new ActionWidget(CHAT_GAMES_SLOT,
                    context -> GuiUtil.createToggleItem(settings.isChatGamesEnabled(), "§bChat Games",
                            "§eClick to toggle"),
                    (click, context) -> toggleChatGames(context.player(), settings)));
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

    private void cycleFilter(Player player) {
        Filter next = switch (filters.getOrDefault(player.getUniqueId(), Filter.ALL)) {
            case ALL -> Filter.SOCIAL;
            case SOCIAL -> Filter.VISUAL;
            case VISUAL -> Filter.COMBAT;
            case COMBAT -> Filter.ALL;
        };
        filters.put(player.getUniqueId(), next);
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
            main.getLeaderboardManager().updateType(LeaderboardType.BALANCE);
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
        openSettingsMenu(player);
    }
}

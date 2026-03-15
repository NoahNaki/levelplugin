package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.commands.DebugCommand;
import me.nakilex.levelplugin.player.attributes.gui.LifeSkillGUI;
import me.nakilex.levelplugin.player.attributes.gui.StatsInventory;
import me.nakilex.levelplugin.player.profile.ProfileEntryUtil;
import me.nakilex.levelplugin.server.ServerSelectionManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StaticItemListener implements Listener {

    private static final ItemStack STATIC_STATS_VIEWER;   // Nether Star (Stats Viewer)
    private static final ItemStack STATIC_LIFE_SKILL;     // Stone Pickaxe (Life Skills)
    private static final ItemStack STATIC_HORSE_SADDLE;   // Saddle (Horse Spawner)
    private static final ItemStack STATIC_QUEST_BOOK;     // Book (Quest Log)
    private static final ItemStack STATIC_COMPASS;        // Compass (Server Selector)
    private static final ItemStack STATIC_CODEX;          // Spyglass (Codex)
    private static final ItemStack STATIC_SETTINGS;       // Comparator (Settings)
    private static final int[] CRAFTING_RAW_SLOTS = {0, 1, 2, 3, 4};
    private static final Set<UUID> PLAYERS_NEEDING_CRAFTING_MENU_REFRESH = ConcurrentHashMap.newKeySet();
    private static volatile boolean craftingMenuRefreshTaskStarted;

    static {
        STATIC_STATS_VIEWER = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = STATIC_STATS_VIEWER.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName(ChatColor.AQUA + "Stats Viewer");
            statsMeta.setLore(TooltipUtil.clickInstructions(null, "to view your stats."));
            STATIC_STATS_VIEWER.setItemMeta(statsMeta);
        }

        STATIC_LIFE_SKILL = StatsInventory.createLifeSkillButton();

        STATIC_HORSE_SADDLE = new ItemStack(Material.SADDLE);
        ItemMeta horseMeta = STATIC_HORSE_SADDLE.getItemMeta();
        if (horseMeta != null) {
            horseMeta.setDisplayName(ChatColor.AQUA + "Horse");
            horseMeta.setLore(TooltipUtil.clickInstructions(null, "to spawn a horse."));
            STATIC_HORSE_SADDLE.setItemMeta(horseMeta);
        }

        STATIC_QUEST_BOOK = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = STATIC_QUEST_BOOK.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.AQUA + "Quest Book");
            bookMeta.setLore(TooltipUtil.clickInstructions(null, "to view your quests."));
            STATIC_QUEST_BOOK.setItemMeta(bookMeta);
        }

        STATIC_COMPASS = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = STATIC_COMPASS.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName(ChatColor.AQUA + "Server Selector");
            compassMeta.setLore(TooltipUtil.clickInstructions(null, "to choose a server."));
            STATIC_COMPASS.setItemMeta(compassMeta);
        }

        STATIC_CODEX = new ItemStack(Material.SPYGLASS);
        ItemMeta codexMeta = STATIC_CODEX.getItemMeta();
        if (codexMeta != null) {
            codexMeta.setDisplayName(ChatColor.YELLOW + "Codex");
            codexMeta.setLore(TooltipUtil.clickInstructions(null, "to review your discoveries."));
            STATIC_CODEX.setItemMeta(codexMeta);
        }

        STATIC_SETTINGS = new ItemStack(Material.COMPARATOR);
        ItemMeta settingsMeta = STATIC_SETTINGS.getItemMeta();
        if (settingsMeta != null) {
            settingsMeta.setDisplayName(ChatColor.AQUA + "Settings");
            settingsMeta.setLore(TooltipUtil.clickInstructions(null, "to configure gameplay options."));
            STATIC_SETTINGS.setItemMeta(settingsMeta);
        }
    }

    private static boolean isManagedStaticItem(ItemStack item) {
        return item != null && (
                item.isSimilar(STATIC_STATS_VIEWER)
                        || item.isSimilar(STATIC_LIFE_SKILL)
                        || item.isSimilar(STATIC_HORSE_SADDLE)
                        || item.isSimilar(STATIC_QUEST_BOOK)
                        || item.isSimilar(STATIC_COMPASS)
                        || item.isSimilar(STATIC_CODEX)
                        || item.isSimilar(STATIC_SETTINGS)
        );
    }

    public static boolean isStaticItem(ItemStack item) {
        return isManagedStaticItem(item);
    }

    public static void giveStaticItems(Player player) {
        player.getInventory().setItem(6, STATIC_HORSE_SADDLE.clone());
    }

    private static ItemStack getCraftingMenuItem(Player player, int rawSlot) {
        return switch (rawSlot) {
            case 0 -> STATIC_STATS_VIEWER.clone();
            case 1 -> STATIC_LIFE_SKILL.clone();
            case 2 -> STATIC_QUEST_BOOK.clone();
            case 3 -> STATIC_CODEX.clone();
            case 4 -> STATIC_SETTINGS.clone();
            default -> null;
        };
    }

    public static ItemStack createCraftingMenuItem(Player player, int rawSlot) {
        ItemStack item = getCraftingMenuItem(player, rawSlot);
        return item == null ? null : item.clone();
    }

    public static void applyCraftingShortcutItems(Player player, CraftingInventory craftingInventory) {
        if (craftingInventory == null) {
            return;
        }
        for (int raw = 1; raw <= 4; raw++) {
            craftingInventory.setItem(raw, createCraftingMenuItem(player, raw));
        }
        ItemStack resultItem = createCraftingMenuItem(player, 0);
        craftingInventory.setResult(resultItem);
        craftingInventory.setItem(0, resultItem == null ? null : resultItem.clone());
    }

    private static boolean hasMissingCraftingShortcutItems(Player player, CraftingInventory craftingInventory) {
        if (craftingInventory == null) {
            return true;
        }
        ItemStack expectedResult = createCraftingMenuItem(player, 0);
        ItemStack renderedResult = craftingInventory.getResult();
        ItemStack slotZeroItem = craftingInventory.getItem(0);
        if (expectedResult != null && (!expectedResult.isSimilar(renderedResult) || !expectedResult.isSimilar(slotZeroItem))) {
            return true;
        }
        for (int raw = 1; raw <= 4; raw++) {
            ItemStack expected = createCraftingMenuItem(player, raw);
            if (expected != null && !expected.isSimilar(craftingInventory.getItem(raw))) {
                return true;
            }
        }
        return false;
    }

    private static void refreshDebugCraftingShortcuts(Main main) {
        for (Player player : main.getServer().getOnlinePlayers()) {
            if (!DebugCommand.isInventoryDebugEnabled(player.getUniqueId())) {
                continue;
            }
            InventoryView view = player.getOpenInventory();
            if (!isCraftingMenuContext(view) || !(view.getTopInventory() instanceof CraftingInventory craftingInventory)) {
                continue;
            }
            if (!hasMissingCraftingShortcutItems(player, craftingInventory)) {
                continue;
            }
            applyCraftingShortcutItems(player, craftingInventory);
            player.updateInventory();
        }
    }

    public static void clearCraftingShortcutItems(CraftingInventory craftingInventory) {
        if (craftingInventory == null) {
            return;
        }
        for (int raw = 1; raw <= 4; raw++) {
            craftingInventory.setItem(raw, null);
        }
        craftingInventory.setResult(null);
        craftingInventory.setItem(0, null);
    }

    private static void reapplyDebugCraftingShortcutsNextTick(Player player) {
        Main main = Main.getInstance();
        if (main == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(main, () -> {
            if (player == null || !player.isOnline() || !DebugCommand.isInventoryDebugEnabled(player.getUniqueId())) {
                return;
            }
            InventoryView view = player.getOpenInventory();
            if (!isCraftingMenuContext(view) || !(view.getTopInventory() instanceof CraftingInventory craftingInventory)) {
                return;
            }
            applyCraftingShortcutItems(player, craftingInventory);
            player.updateInventory();
        }, 1L);
    }

    private static boolean isManagedCraftingRawSlot(int rawSlot) {
        for (int slot : CRAFTING_RAW_SLOTS) {
            if (slot == rawSlot) {
                return true;
            }
        }
        return false;
    }

    public static boolean runCraftingSlotAction(Player player, int rawSlot, boolean delayOneTick) {
        ItemStack menuItem = getCraftingMenuItem(player, rawSlot);
        if (menuItem == null) {
            return false;
        }
        handleStaticAction(player, menuItem, delayOneTick);
        return true;
    }

    private static boolean isCraftingMenuContext(InventoryView view) {
        return view != null && view.getTopInventory() != null
                && view.getTopInventory().getType() == InventoryType.CRAFTING;
    }

    private static boolean shouldSkipCraftingMenu(Player player) {
        return true;
    }

    private static void setCraftingMenuSlots(Player player, InventoryView view, boolean applyMenu) {
        if (!isCraftingMenuContext(view)) {
            return;
        }
        CraftingInventory craftingInventory = (CraftingInventory) view.getTopInventory();
        craftingInventory.setResult(applyMenu ? getCraftingMenuItem(player, 0) : null);
        for (int slot : CRAFTING_RAW_SLOTS) {
            if (slot == 0) {
                continue;
            }
            craftingInventory.setItem(slot, applyMenu ? getCraftingMenuItem(player, slot) : null);
        }
    }

    private static void refreshCraftingMenu(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        InventoryView view = player.getOpenInventory();
        if (!isCraftingMenuContext(view)) {
            return;
        }
        boolean applyMenu = !shouldSkipCraftingMenu(player);
        setCraftingMenuSlots(player, view, applyMenu);
        player.setItemOnCursor(null);
        if (applyMenu) {
            player.updateInventory();
        }
    }

    private static void clearCraftingMenu(Player player, InventoryView view) {
        setCraftingMenuSlots(player, view, false);
    }

    private static void queueCraftingMenuRefresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PLAYERS_NEEDING_CRAFTING_MENU_REFRESH.add(player.getUniqueId());
    }

    private static void ensureCraftingMenuRefreshTaskRunning() {
        Main main = Main.getInstance();
        if (main == null || craftingMenuRefreshTaskStarted) {
            return;
        }
        craftingMenuRefreshTaskStarted = true;
        main.getServer().getScheduler().runTaskTimer(main, () -> {
            if (PLAYERS_NEEDING_CRAFTING_MENU_REFRESH.isEmpty()) {
                refreshDebugCraftingShortcuts(main);
                return;
            }
            for (UUID uuid : Set.copyOf(PLAYERS_NEEDING_CRAFTING_MENU_REFRESH)) {
                Player player = main.getServer().getPlayer(uuid);
                PLAYERS_NEEDING_CRAFTING_MENU_REFRESH.remove(uuid);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                refreshCraftingMenu(player);
            }

            refreshDebugCraftingShortcuts(main);
        }, 1L, 2L);
    }

    public static void giveHubItems(Player player) {
        ProfileEntryUtil.clearInventory(player);
        player.getInventory().setItem(4, STATIC_COMPASS.clone());
    }

    public static void clearStaticItems(Player player) {
        if (player == null) {
            return;
        }
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isStaticItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public static void applyWorldLoadout(Player player) {
        if (player == null) {
            return;
        }
        if (WorldExclusionUtil.isExcluded(player)) {
            clearStaticItems(player);
            return;
        }
        Main main = Main.getInstance();
        if (main != null) {
            ServerSelectionManager manager = main.getServerSelectionManager();
            if (manager != null && manager.isHubWorld(player.getWorld())) {
                giveHubItems(player);
                me.nakilex.levelplugin.utils.BetterHudUtil.removeHud(player);
                return;
            }
        }
        giveStaticItems(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        ensureCraftingMenuRefreshTaskRunning();
        applyWorldLoadout(p);
        queueCraftingMenuRefresh(p);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PLAYERS_NEEDING_CRAFTING_MENU_REFRESH.remove(player.getUniqueId());
        clearStaticItems(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        queueCraftingMenuRefresh(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        queueCraftingMenuRefresh(event.getPlayer());
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        queueCraftingMenuRefresh(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (DebugCommand.isInventoryDebugEnabled(player.getUniqueId())
                && isCraftingMenuContext(event.getView())
                && event.getView().getTopInventory() instanceof CraftingInventory craftingInventory) {
            applyCraftingShortcutItems(player, craftingInventory);
            player.updateInventory();
            reapplyDebugCraftingShortcutsNextTick(player);
            return;
        }
        if (shouldSkipCraftingMenu(player) || !isCraftingMenuContext(event.getView())) {
            return;
        }
        refreshCraftingMenu(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!shouldSkipCraftingMenu(player) && isCraftingMenuContext(event.getView()) && isManagedCraftingRawSlot(event.getRawSlot())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                event.setCursor(null);
            }
            player.setItemOnCursor(null);
            player.updateInventory();
            if ((event.getCursor() == null || event.getCursor().getType().isAir())) {
                runCraftingSlotAction(player, event.getRawSlot(), true);
            }
            queueCraftingMenuRefresh(player);
            return;
        }

        if (DebugCommand.isInventoryDebugEnabled(player.getUniqueId())
                && isCraftingMenuContext(event.getView())
                && isManagedCraftingRawSlot(event.getRawSlot())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                event.setCursor(null);
            }
            player.setItemOnCursor(null);
            player.updateInventory();
            if (event.getCursor() == null || event.getCursor().getType().isAir()) {
                runCraftingSlotAction(player, event.getRawSlot(), true);
            }
            reapplyDebugCraftingShortcutsNextTick(player);
            return;
        }

        ItemStack curr = event.getCurrentItem();
        if (isManagedStaticItem(curr)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (DebugCommand.isInventoryDebugEnabled(player.getUniqueId())
                && isCraftingMenuContext(event.getView())
                && event.getView().getTopInventory() instanceof CraftingInventory craftingInventory) {
            player.setItemOnCursor(null);
            clearCraftingShortcutItems(craftingInventory);
            player.updateInventory();
            return;
        }
        if (!isCraftingMenuContext(event.getView()) || shouldSkipCraftingMenu(player)) {
            return;
        }
        player.setItemOnCursor(null);
        clearCraftingMenu(player, event.getView());
        queueCraftingMenuRefresh(player);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isCraftingMenuContext(event.getView())) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (isManagedCraftingRawSlot(rawSlot)) {
                event.setCancelled(true);
                queueCraftingMenuRefresh(player);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isManagedStaticItem(dropped)
                || me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(dropped)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack m = event.getMainHandItem();
        ItemStack o = event.getOffHandItem();
        if (isManagedStaticItem(m) || isManagedStaticItem(o)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.PHYSICAL) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (isManagedStaticItem(inHand)) {
            handleStaticAction(player, inHand, false);
            event.setCancelled(true);
        }
    }

    private static void runStaticAction(Player player, boolean delayOneTick, Runnable action) {
        if (player == null || action == null) {
            return;
        }
        if (!delayOneTick) {
            action.run();
            return;
        }
        Main main = Main.getInstance();
        if (main == null) {
            action.run();
            return;
        }
        Bukkit.getScheduler().runTaskLater(main, () -> {
            if (player.isOnline()) {
                action.run();
            }
        }, 1L);
    }

    private static void handleStaticAction(Player player, ItemStack item, boolean delayOneTick) {
        if (item.isSimilar(STATIC_STATS_VIEWER)) {
            runStaticAction(player, delayOneTick, () -> player.performCommand("stats"));
            return;
        }
        if (item.isSimilar(STATIC_LIFE_SKILL)) {
            runStaticAction(player, delayOneTick, () -> LifeSkillGUI.open(player));
            return;
        }
        if (item.isSimilar(STATIC_HORSE_SADDLE)) {
            runStaticAction(player, delayOneTick, () -> player.performCommand("horse spawn"));
            return;
        }
        if (item.isSimilar(STATIC_QUEST_BOOK)) {
            runStaticAction(player, delayOneTick, () -> player.performCommand("quest"));
            return;
        }
        if (item.isSimilar(STATIC_CODEX)) {
            runStaticAction(player, delayOneTick, () -> player.performCommand("codex"));
            return;
        }
        if (item.isSimilar(STATIC_SETTINGS)) {
            runStaticAction(player, delayOneTick, () -> player.performCommand("settings"));
            return;
        }
        if (item.isSimilar(STATIC_COMPASS)) {
            runStaticAction(player, delayOneTick, () -> {
                Main main = Main.getInstance();
                if (main != null && main.getServerSelectionManager() != null) {
                    main.getServerSelectionManager().openSelector(player);
                }
            });
        }
    }
}

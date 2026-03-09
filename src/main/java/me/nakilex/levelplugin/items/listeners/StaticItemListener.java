package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.profile.ProfileEntryUtil;
import me.nakilex.levelplugin.server.ServerSelectionManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class StaticItemListener implements Listener {

    private static final ItemStack STATIC_ITEM;           // Nether Star (Stats Viewer)
    private static final ItemStack STATIC_HORSE_SADDLE;   // Saddle (Horse Spawner)
    private static final ItemStack STATIC_QUEST_BOOK;     // Book (Quest Log)
    private static final ItemStack STATIC_COMPASS;        // Compass (Server Selector)
    private static final ItemStack STATIC_CODEX;          // Spyglass (Codex)
    private static final ItemStack STATIC_SETTINGS;       // Comparator (Settings)

    static {
        // --- Stats Viewer (Nether Star) ---
        STATIC_ITEM = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = STATIC_ITEM.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName(ChatColor.AQUA + "Stats Viewer");
            statsMeta.setLore(TooltipUtil.clickInstructions(null, "to view your stats."));
            STATIC_ITEM.setItemMeta(statsMeta);
        }

        // --- Horse Spawner (Saddle) ---
        STATIC_HORSE_SADDLE = new ItemStack(Material.SADDLE);
        ItemMeta horseMeta = STATIC_HORSE_SADDLE.getItemMeta();
        if (horseMeta != null) {
            horseMeta.setDisplayName(ChatColor.AQUA + "Horse");
            horseMeta.setLore(TooltipUtil.clickInstructions(null, "to spawn a horse."));
            STATIC_HORSE_SADDLE.setItemMeta(horseMeta);
        }

        // --- Quest Book (must match your BetonQuest item) ---
        STATIC_QUEST_BOOK = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = STATIC_QUEST_BOOK.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.AQUA + "Quest Book");
            bookMeta.setLore(TooltipUtil.clickInstructions(null, "to view your quests."));
            STATIC_QUEST_BOOK.setItemMeta(bookMeta);
        }

        // --- Server Selector (Compass) ---
        STATIC_COMPASS = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = STATIC_COMPASS.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName(ChatColor.AQUA + "Server Selector");
            compassMeta.setLore(TooltipUtil.clickInstructions(null, "to choose a server."));
            STATIC_COMPASS.setItemMeta(compassMeta);
        }

        // --- Codex (Spyglass) ---
        STATIC_CODEX = new ItemStack(Material.SPYGLASS);
        ItemMeta codexMeta = STATIC_CODEX.getItemMeta();
        if (codexMeta != null) {
            codexMeta.setDisplayName(ChatColor.YELLOW + "Codex");
            codexMeta.setLore(TooltipUtil.clickInstructions(null, "to review your discoveries."));
            STATIC_CODEX.setItemMeta(codexMeta);
        }

        // --- Settings (Comparator) ---
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
                item.isSimilar(STATIC_ITEM)
                        || item.isSimilar(STATIC_HORSE_SADDLE)
                        || item.isSimilar(STATIC_QUEST_BOOK)
                        || item.isSimilar(STATIC_COMPASS)
                        || item.isSimilar(STATIC_CODEX)
                        || item.isSimilar(STATIC_SETTINGS)
        );
    }

    /**
     * Determine if the provided item is one of the static menu items.
     */
    public static boolean isStaticItem(ItemStack item) {
        if (item == null) return false;
        return item.isSimilar(STATIC_ITEM)
                || item.isSimilar(STATIC_HORSE_SADDLE)
                || item.isSimilar(STATIC_QUEST_BOOK)
                || item.isSimilar(STATIC_COMPASS)
                || item.isSimilar(STATIC_CODEX)
                || item.isSimilar(STATIC_SETTINGS);
    }

    /**
     * Give the standard static items to the player's hotbar.
     */
    public static void giveStaticItems(Player player) {
        player.getInventory().setItem(6, STATIC_HORSE_SADDLE.clone());
        setCraftingMenuItems(player);
    }

    private static void setCraftingMenuItems(Player player) {
        ItemStack[] extra = player.getInventory().getExtraContents();
        if (extra == null || extra.length == 0) {
            return;
        }

        ItemStack[] updated = extra.clone();
        ItemStack[] menuItems = {
                STATIC_ITEM.clone(),
                STATIC_QUEST_BOOK.clone(),
                STATIC_CODEX.clone(),
                STATIC_SETTINGS.clone()
        };

        int limit = Math.min(updated.length, menuItems.length);
        for (int i = 0; i < limit; i++) {
            updated[i] = menuItems[i];
        }

        player.getInventory().setExtraContents(updated);
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
        ItemStack[] extra = player.getInventory().getExtraContents();
        boolean changed = false;
        for (int i = 0; i < extra.length; i++) {
            if (isManagedStaticItem(extra[i])) {
                extra[i] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setExtraContents(extra);
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
        applyWorldLoadout(p);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack curr = event.getCurrentItem();
        if (curr != null && (
            isManagedStaticItem(curr)
        )) {
            event.setCancelled(true);
            if (event.getSlotType() == InventoryType.SlotType.CRAFTING) {
                handleStaticAction((Player) event.getWhoClicked(), curr);
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
            handleStaticAction(player, inHand);
            event.setCancelled(true);
        }

    }

    private static void handleStaticAction(Player player, ItemStack item) {
        if (item.isSimilar(STATIC_ITEM)) {
            player.performCommand("stats");
            return;
        }
        if (item.isSimilar(STATIC_HORSE_SADDLE)) {
            player.performCommand("horse spawn");
            return;
        }
        if (item.isSimilar(STATIC_QUEST_BOOK)) {
            player.performCommand("quest");
            return;
        }
        if (item.isSimilar(STATIC_CODEX)) {
            player.performCommand("codex");
            return;
        }
        if (item.isSimilar(STATIC_SETTINGS)) {
            player.performCommand("settings");
            return;
        }
        if (item.isSimilar(STATIC_COMPASS)) {
            Main main = Main.getInstance();
            if (main != null && main.getServerSelectionManager() != null) {
                main.getServerSelectionManager().openSelector(player);
            }
        }
    }
}

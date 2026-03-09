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
    private static final ItemStack STATIC_CODEX;          // Enchanted Book (Codex)
    private static final ItemStack STATIC_SETTINGS;       // Redstone Torch (Settings)
    private static final ItemStack STATIC_COMPASS;        // Compass (Server Selector)

    static {
        STATIC_ITEM = createStaticItem(Material.NETHER_STAR, "Stats Viewer", "to view your stats.");

        // --- Horse Spawner (Saddle) ---
        STATIC_HORSE_SADDLE = createStaticItem(Material.SADDLE, "Horse", "to spawn a horse.");

        // --- Quest Book (must match your BetonQuest item) ---
        STATIC_QUEST_BOOK = createStaticItem(Material.BOOK, "Quest Book", "to view your quests.");
        STATIC_CODEX = createStaticItem(Material.ENCHANTED_BOOK, "Codex", "to open your codex.");
        STATIC_SETTINGS = createStaticItem(Material.REDSTONE_TORCH, "Settings", "to open settings.");

        // --- Server Selector (Compass) ---
        STATIC_COMPASS = createStaticItem(Material.COMPASS, "Server Selector", "to choose a server.");
    }

    private static ItemStack createStaticItem(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.AQUA + name);
        meta.setLore(TooltipUtil.clickInstructions(null, action));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Determine if the provided item is one of the static menu items.
     */
    public static boolean isStaticItem(ItemStack item) {
        if (item == null) return false;
        return item.isSimilar(STATIC_ITEM)
                || item.isSimilar(STATIC_HORSE_SADDLE)
                || item.isSimilar(STATIC_QUEST_BOOK)
                || item.isSimilar(STATIC_CODEX)
                || item.isSimilar(STATIC_SETTINGS)
                || item.isSimilar(STATIC_COMPASS);
    }

    /**
     * Give the standard static items to the player inventory (hotbar + crafting grid).
     */
    public static void giveStaticItems(Player player) {
        ItemStack[] extra = player.getInventory().getExtraContents();
        if (extra == null || extra.length < 5) {
            extra = new ItemStack[5];
        }
        extra[0] = null;
        extra[1] = STATIC_ITEM.clone();
        extra[2] = STATIC_QUEST_BOOK.clone();
        extra[3] = STATIC_CODEX.clone();
        extra[4] = STATIC_SETTINGS.clone();
        player.getInventory().setExtraContents(extra);

        player.getInventory().setItem(6, STATIC_HORSE_SADDLE.clone());
    }

    public static void giveHubItems(Player player) {
        ProfileEntryUtil.clearInventory(player);
        player.getInventory().setExtraContents(new ItemStack[5]);
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
        if (extra == null) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < extra.length; i++) {
            if (isStaticItem(extra[i])) {
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
        if (isStaticItem(curr)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player
                    && event.getClickedInventory() != null
                    && event.getClickedInventory().getType() == InventoryType.PLAYER
                    && event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
                useStaticItem(player, curr);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.isSimilar(STATIC_ITEM)
            || dropped.isSimilar(STATIC_HORSE_SADDLE)
            || dropped.isSimilar(STATIC_QUEST_BOOK)
            || dropped.isSimilar(STATIC_CODEX)
            || dropped.isSimilar(STATIC_SETTINGS)
            || dropped.isSimilar(STATIC_COMPASS)
            || me.nakilex.levelplugin.items.utils.ItemUtil.isSoulbound(dropped)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack m = event.getMainHandItem();
        ItemStack o = event.getOffHandItem();
        if ((m != null && (
                m.isSimilar(STATIC_ITEM) ||
                m.isSimilar(STATIC_HORSE_SADDLE) ||
                m.isSimilar(STATIC_QUEST_BOOK) ||
                m.isSimilar(STATIC_CODEX) ||
                m.isSimilar(STATIC_SETTINGS) ||
                m.isSimilar(STATIC_COMPASS)))
            || (o != null && (
            o.isSimilar(STATIC_ITEM) ||
                o.isSimilar(STATIC_HORSE_SADDLE) ||
                o.isSimilar(STATIC_QUEST_BOOK) ||
                o.isSimilar(STATIC_CODEX) ||
                o.isSimilar(STATIC_SETTINGS) ||
                o.isSimilar(STATIC_COMPASS)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() == Action.PHYSICAL) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (useStaticItem(player, inHand)) {
            event.setCancelled(true);
        }

    }

    private boolean useStaticItem(Player player, ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.isSimilar(STATIC_ITEM)) {
            player.performCommand("stats");
            return true;
        }
        if (item.isSimilar(STATIC_HORSE_SADDLE)) {
            player.performCommand("horse spawn");
            return true;
        }
        if (item.isSimilar(STATIC_QUEST_BOOK)) {
            player.performCommand("quest");
            return true;
        }
        if (item.isSimilar(STATIC_CODEX)) {
            player.performCommand("codex");
            return true;
        }
        if (item.isSimilar(STATIC_SETTINGS)) {
            player.performCommand("settings");
            return true;
        }
        if (item.isSimilar(STATIC_COMPASS)) {
            Main main = Main.getInstance();
            if (main != null && main.getServerSelectionManager() != null) {
                main.getServerSelectionManager().openSelector(player);
            }
            return true;
        }
        return false;

    }
}

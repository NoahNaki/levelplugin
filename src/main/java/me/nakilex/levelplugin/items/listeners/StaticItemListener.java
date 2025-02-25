package me.nakilex.levelplugin.items.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class StaticItemListener implements Listener {

    private static final ItemStack STATIC_ITEM;           // Nether Star (Stats Viewer)
    private static final ItemStack STATIC_HORSE_SADDLE;     // Saddle (Horse Spawner)
    private static final ItemStack STATIC_QUEST_BOOK;       // Book (Quest Log)

    static {
        // Initialize the static Nether Star (Stats Viewer)
        STATIC_ITEM = new ItemStack(Material.NETHER_STAR);
        ItemMeta netherMeta = STATIC_ITEM.getItemMeta();
        if (netherMeta != null) {
            netherMeta.setDisplayName(ChatColor.AQUA + "Stats Viewer");
            netherMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Right-click to view your stats."));
            STATIC_ITEM.setItemMeta(netherMeta);
        }

        // Initialize the static Saddle (Horse Spawner)
        STATIC_HORSE_SADDLE = new ItemStack(Material.SADDLE);
        ItemMeta horseMeta = STATIC_HORSE_SADDLE.getItemMeta();
        if (horseMeta != null) {
            horseMeta.setDisplayName(ChatColor.AQUA + "Horse");
            horseMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Right-click to spawn a horse."));
            STATIC_HORSE_SADDLE.setItemMeta(horseMeta);
        }

        // Initialize the static Book (Quest Log)
        STATIC_QUEST_BOOK = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = STATIC_QUEST_BOOK.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.AQUA + "Quest Book");
            bookMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Right-click to view your quests."));
            STATIC_QUEST_BOOK.setItemMeta(bookMeta);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Set the static items in the hotbar:
        // Place the Horse Saddle in slot 6 (index 5)
        // Place the Book in slot 7 (index 6)
        // Place the Nether Star in slot 9 (index 8)
        player.getInventory().setItem(6, STATIC_HORSE_SADDLE);
        player.getInventory().setItem(7, STATIC_QUEST_BOOK);
        player.getInventory().setItem(8, STATIC_ITEM);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Prevent the static items from being moved
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null &&
            (currentItem.isSimilar(STATIC_ITEM) ||
                currentItem.isSimilar(STATIC_HORSE_SADDLE) ||
                currentItem.isSimilar(STATIC_QUEST_BOOK))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Prevent the static items from being dropped
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (droppedItem.isSimilar(STATIC_ITEM) ||
            droppedItem.isSimilar(STATIC_HORSE_SADDLE) ||
            droppedItem.isSimilar(STATIC_QUEST_BOOK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Cancel the hand swap if either the main hand or offhand holds a static item
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        if ((mainHand != null && (mainHand.isSimilar(STATIC_ITEM) ||
            mainHand.isSimilar(STATIC_HORSE_SADDLE) ||
            mainHand.isSimilar(STATIC_QUEST_BOOK))) ||
            (offHand != null && (offHand.isSimilar(STATIC_ITEM) ||
                offHand.isSimilar(STATIC_HORSE_SADDLE) ||
                offHand.isSimilar(STATIC_QUEST_BOOK)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the player right-clicked with one of the static items
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.isSimilar(STATIC_ITEM)) {
            // Execute the /stats command
            player.performCommand("stats");
            event.setCancelled(true);
        } else if (itemInHand.isSimilar(STATIC_HORSE_SADDLE)) {
            // Execute the /horse spawn command
            player.performCommand("horse spawn");
            event.setCancelled(true);
        } else if (itemInHand.isSimilar(STATIC_QUEST_BOOK)) {
            // Execute the /quests command
            player.performCommand("quests");
            event.setCancelled(true);
        }
    }
}

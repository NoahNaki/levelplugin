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

import java.util.Arrays;
import java.util.Collections;

public class StaticItemListener implements Listener {

    private static final ItemStack STATIC_ITEM;           // Nether Star (Stats Viewer)
    private static final ItemStack STATIC_HORSE_SADDLE;   // Saddle (Horse Spawner)
    private static final ItemStack STATIC_QUEST_BOOK;     // Book (Quest Log)

    static {
        // --- Stats Viewer (Nether Star) ---
        STATIC_ITEM = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = STATIC_ITEM.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName(ChatColor.AQUA + "Stats Viewer");
            statsMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Right-click to view your stats."));
            STATIC_ITEM.setItemMeta(statsMeta);
        }

        // --- Horse Spawner (Saddle) ---
        STATIC_HORSE_SADDLE = new ItemStack(Material.SADDLE);
        ItemMeta horseMeta = STATIC_HORSE_SADDLE.getItemMeta();
        if (horseMeta != null) {
            horseMeta.setDisplayName(ChatColor.AQUA + "Horse");
            horseMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Right-click to spawn a horse."));
            STATIC_HORSE_SADDLE.setItemMeta(horseMeta);
        }

        // --- Quest Book (must match your BetonQuest item) ---
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
        Player p = event.getPlayer();
        p.getInventory().setItem(6, STATIC_HORSE_SADDLE);
        p.getInventory().setItem(7, STATIC_QUEST_BOOK);
        p.getInventory().setItem(8, STATIC_ITEM);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack curr = event.getCurrentItem();
        if (curr != null && (
            curr.isSimilar(STATIC_ITEM) ||
                curr.isSimilar(STATIC_HORSE_SADDLE) ||
                curr.isSimilar(STATIC_QUEST_BOOK)
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.isSimilar(STATIC_ITEM)
            || dropped.isSimilar(STATIC_HORSE_SADDLE)
            || dropped.isSimilar(STATIC_QUEST_BOOK)) {
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
                m.isSimilar(STATIC_QUEST_BOOK)))
            || (o != null && (
            o.isSimilar(STATIC_ITEM) ||
                o.isSimilar(STATIC_HORSE_SADDLE) ||
                o.isSimilar(STATIC_QUEST_BOOK)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (inHand != null && inHand.isSimilar(STATIC_ITEM)) {
            player.performCommand("stats");
            event.setCancelled(true);

        } else if (inHand != null && inHand.isSimilar(STATIC_HORSE_SADDLE)) {
            player.performCommand("horse spawn");
            event.setCancelled(true);

        }
        // ← NO "else if (inHand.isSimilar(STATIC_QUEST_BOOK))" block here!
        // we let BetonQuest itself see the click and open the GUI.
    }
}

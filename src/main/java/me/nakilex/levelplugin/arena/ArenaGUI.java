package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple GUI to display arena rank and join/leave queue buttons.
 */
public class ArenaGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Arena";
    private final ArenaManager arena;

    public ArenaGUI(ArenaManager arena) {
        this.arena = arena;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();

        ArenaManager.Rating rating = arena.getRating(player.getUniqueId());
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta meta = stats.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + arena.getTierName(rating.rankPoints));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rank Points: " + rating.rankPoints);
            lore.add(ChatColor.GRAY + "MMR: " + rating.mmr);
            meta.setLore(lore);
            stats.setItemMeta(meta);
        }
        inv.setItem(11, stats);

        boolean queued = arenaPlayerQueued(player.getUniqueId());
        inv.setItem(15, createQueueItem(queued));

        player.openInventory(inv);
    }

    private boolean arenaPlayerQueued(UUID id) {
        return Main.getInstance().getArenaManager().queueContains(id);
    }

    private ItemStack createQueueItem(boolean queued) {
        Material mat = queued ? Material.BARRIER : Material.DIAMOND_SWORD;
        String name = queued ? ChatColor.RED + "Leave Queue" : ChatColor.GREEN + "Join Queue";
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + (queued ? "Click to leave" : "Click to join"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getSlot() == 15) {
            if (arenaPlayerQueued(player.getUniqueId())) {
                arena.leaveQueue(player);
            } else {
                arena.joinQueue(player);
            }
            open(player);
        }
    }
}

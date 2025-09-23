package me.nakilex.levelplugin.arena.gui;

import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Lightweight queue menu mirroring the modern GUI style used across the
 * plugin. Players can join/leave the arena queue and see the live count
 * in the tooltip formatted as "&a&lN &7players in queue!" as requested.
 */
public class ArenaQueueGUI implements Listener {
    private static final int GUI_SIZE = 27;
    private static final String TITLE = ChatColor.BLACK + "<glyph:crossedswords_icon> Arena Queue";
    private static final int QUEUE_SLOT = 13;

    private final ArenaQueueManager queueManager;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public ArenaQueueGUI(ArenaQueueManager queueManager) {
        this.queueManager = queueManager;
    }

    /**
     * Open the arena queue menu for the player.
     */
    public void open(Player player) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        inv.setItem(QUEUE_SLOT, createQueueButton(player.getUniqueId()));
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    /**
     * Update all currently open inventories. Invoked after queue actions or
     * by external callers (commands) to refresh the live count.
     */
    public void refresh() {
        refreshOpenInventories();
    }

    private ItemStack createQueueButton(UUID viewerId) {
        boolean queued = viewerId != null && queueManager.isQueued(viewerId);
        String name = queued
                ? ChatColor.RED + "" + ChatColor.BOLD + "Leave Arena Queue"
                : ChatColor.GREEN + "" + ChatColor.BOLD + "Join Arena Queue";

        ItemStack item = GuiUtil.getNexoItem("swords_icon", name);
        if (item.getType() == Material.BARRIER) {
            item = new ItemStack(Material.IRON_SWORD);
            ItemMeta fallback = item.getItemMeta();
            if (fallback != null) {
                fallback.setDisplayName(name);
                item.setItemMeta(fallback);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Queue up to battle challengers.");
            lore.add("");
            lore.add(formatQueueStatusLine());
            lore.add("");
            if (queued) {
                lore.addAll(TooltipUtil.clickInstructions("to leave the queue", null));
            } else {
                lore.addAll(TooltipUtil.clickInstructions("to join the queue", null));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatQueueStatusLine() {
        int size = queueManager.getQueueSize();
        return ChatColor.GREEN + "" + ChatColor.BOLD + size + ChatColor.GRAY + " players in queue!";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() != QUEUE_SLOT) return;

        UUID id = player.getUniqueId();
        if (queueManager.isQueued(id)) {
            queueManager.leave(id);
            send(player, MessageType.INFO, "You left the arena queue.");
        } else {
            queueManager.join(player);
            send(player, MessageType.SUCCESS, "You joined the arena queue.");
        }
        refreshOpenInventories();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (queueManager.leave(id)) {
            refreshOpenInventories();
        }
        openInventories.remove(id);
    }

    private void refreshOpenInventories() {
        Iterator<Map.Entry<UUID, Inventory>> iterator = openInventories.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Inventory> entry = iterator.next();
            UUID viewerId = entry.getKey();
            Inventory inv = entry.getValue();
            boolean stillOpen = false;
            for (HumanEntity viewer : inv.getViewers()) {
                if (viewer.getUniqueId().equals(viewerId)) {
                    stillOpen = true;
                    break;
                }
            }
            if (!stillOpen) {
                iterator.remove();
                continue;
            }
            inv.setItem(QUEUE_SLOT, createQueueButton(viewerId));
        }
    }
}

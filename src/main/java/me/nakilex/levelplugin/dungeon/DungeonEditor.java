package me.nakilex.levelplugin.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonEditor implements Listener {
    private final DungeonManager manager;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public DungeonEditor(DungeonManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Dungeon Editor");
        ItemStack filler = new ItemStack(Material.WHITE_WOOL);
        ItemMeta m = filler.getItemMeta();
        if (m != null) m.setDisplayName(ChatColor.GRAY + "Empty");
        filler.setItemMeta(m);
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);
        Session s = new Session(player, inv);
        sessions.put(player.getUniqueId(), s);
        player.openInventory(inv);
    }

    private Inventory createSelectInv() {
        Inventory inv = Bukkit.createInventory(null, 9, "Select Room");
        ItemStack entrance = new ItemStack(Material.LIME_WOOL);
        ItemMeta eMeta = entrance.getItemMeta();
        if (eMeta != null) eMeta.setDisplayName(ChatColor.GREEN + "Entrance");
        entrance.setItemMeta(eMeta);
        inv.setItem(0, entrance);

        ItemStack hall = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta hMeta = hall.getItemMeta();
        if (hMeta != null) hMeta.setDisplayName(ChatColor.YELLOW + "Basic Room");
        hall.setItemMeta(hMeta);
        inv.setItem(1, hall);
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        UUID id = e.getWhoClicked().getUniqueId();
        Session s = sessions.get(id);
        if (s == null) return;
        if (e.getInventory().equals(s.layoutInv)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            if (e.getClick().isLeftClick()) {
                s.pendingSlot = e.getRawSlot();
                e.getWhoClicked().openInventory(createSelectInv());
            }
        } else if (s.pendingSlot >= 0 && e.getView().getTitle().contains("Select Room")) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            int slot = s.pendingSlot;
            s.pendingSlot = -1;
            int x = slot % 9;
            int y = slot / 9;
            if (item.getType() == Material.LIME_WOOL) {
                if (s.layout.hasEntrance()) {
                    e.getWhoClicked().sendMessage(ChatColor.RED + "Entrance already set");
                } else {
                    s.layout.set(x, y, RoomType.ENTRANCE);
                    ItemStack it = new ItemStack(Material.LIME_WOOL);
                    ItemMeta im = it.getItemMeta();
                    if (im != null) im.setDisplayName(ChatColor.GREEN + "Entrance");
                    it.setItemMeta(im);
                    s.layoutInv.setItem(slot, it);
                }
            } else if (item.getType() == Material.YELLOW_WOOL) {
                if (!s.layout.hasEntrance()) {
                    e.getWhoClicked().sendMessage(ChatColor.RED + "Place entrance first");
                } else if (!hasAdjacent(s.layout, x, y)) {
                    e.getWhoClicked().sendMessage(ChatColor.RED + "Must be adjacent to another room");
                } else {
                    s.layout.set(x, y, RoomType.HALLWAY);
                    ItemStack it = new ItemStack(Material.YELLOW_WOOL);
                    ItemMeta im = it.getItemMeta();
                    if (im != null) im.setDisplayName(ChatColor.YELLOW + "Basic Room");
                    it.setItemMeta(im);
                    s.layoutInv.setItem(slot, it);
                }
            }
            e.getWhoClicked().openInventory(s.layoutInv);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null) return;
        if (e.getInventory().equals(s.layoutInv)) {
            s.awaitingName = true;
            e.getPlayer().sendMessage(ChatColor.YELLOW + "Type dungeon name in chat or 'Cancel'");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null || !s.awaitingName) return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            e.getPlayer().sendMessage(ChatColor.RED + "Creation cancelled");
            sessions.remove(e.getPlayer().getUniqueId());
            return;
        }
        String display = msg;
        String key = DungeonManager.normalizeKey(display);
        if (manager.layoutExists(display)) {
            e.getPlayer().sendMessage(ChatColor.RED + "A dungeon with that name already exists.");
            sessions.remove(e.getPlayer().getUniqueId());
            return;
        }
        manager.saveLayout(e.getPlayer(), key, display, s.layout);
        e.getPlayer().sendMessage(ChatColor.GREEN + "Dungeon layout saved as '" + key + "'");
        sessions.remove(e.getPlayer().getUniqueId());
    }

    private boolean hasAdjacent(DungeonLayout layout, int x, int y) {
        return layout.get(x + 1, y) != RoomType.NONE ||
               layout.get(x - 1, y) != RoomType.NONE ||
               layout.get(x, y + 1) != RoomType.NONE ||
               layout.get(x, y - 1) != RoomType.NONE;
    }

    private static class Session {
        final Player player;
        final Inventory layoutInv;
        final DungeonLayout layout = new DungeonLayout();
        int pendingSlot = -1;
        boolean awaitingName = false;
        Session(Player player, Inventory inv) { this.player = player; this.layoutInv = inv; }
    }
}

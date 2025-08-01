package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MobCodexGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Codex";
    private static final int SIZE = 54;

    private final CodexManager manager;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    public MobCodexGUI(CodexManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, filler);
        }
        inv.setItem(4, createInfoBook(player.getUniqueId()));

        List<String> mobs = manager.getAllMobKeys();
        int slot = 9;
        for (String key : mobs) {
            if (slot >= 45) break;
            inv.setItem(slot++, createMobIcon(player.getUniqueId(), key));
        }

        player.openInventory(inv);
    }

    private ItemStack createInfoBook(UUID id) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Discoveries");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Mobs: " + ChatColor.WHITE
                    + manager.getDiscoveredMobCount(id) + "/" + manager.getTotalMobCount());
            lore.add(ChatColor.GRAY + "Locations: " + ChatColor.WHITE + "0/0");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack createMobIcon(UUID id, String key) {
        boolean discovered = manager.hasDiscovered(id, key);
        ItemStack item = new ItemStack(discovered ? Material.SKELETON_SKULL : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (discovered) {
                meta.setDisplayName(ChatColor.GREEN + key);
                int discoveredCount = manager.getDiscoveredMobCount(id);
                int total = manager.getTotalMobCount();
                double percent = total == 0 ? 0 : (double) discoveredCount / total;
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Category: Mobs");
                lore.add(ChatColor.WHITE + "Each mob that you've discovered will be listed here.");
                lore.add(ChatColor.GRAY + "Unlocked " + ChatColor.YELLOW + discoveredCount
                        + ChatColor.GRAY + "/" + ChatColor.YELLOW + total + " "
                        + progressBar(percent) + ChatColor.GRAY + " (" + ChatColor.YELLOW
                        + Math.round(percent * 100) + "%" + ChatColor.GRAY + ")");
                lore.add(ChatColor.GRAY + "Kills: " + ChatColor.WHITE + manager.getKillCount(id, key));
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.DARK_GRAY + "???");
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String progressBar(double progress) {
        int totalBars = 10;
        int filled = (int) Math.round(progress * totalBars);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) bar.append(ChatColor.GREEN).append('l');
            else bar.append(ChatColor.RED).append('l');
        }
        bar.append(ChatColor.GRAY).append(']');
        return bar.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(TITLE)) {
            e.setCancelled(true);
        }
    }
}

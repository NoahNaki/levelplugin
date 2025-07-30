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

import me.nakilex.levelplugin.codex.MobEssence;

public class CodexGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "Mob Codex";
    private static final int SIZE = 54;
    private static final String ESSENCE_TITLE = ChatColor.DARK_GREEN + "Essences - ";

    private final CodexManager manager;
    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

    public CodexGUI(CodexManager manager) {
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        GuiUtil.fillBorder(inv, filler);
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
        ItemStack book = GuiUtil.getNexoItem("info", ChatColor.GOLD + "Discoveries");
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Mobs: " + ChatColor.WHITE
                    + manager.getDiscoveredMobCount(id) + "/" + manager.getTotalMobCount());
            lore.add(ChatColor.GRAY + "Locations: " + ChatColor.WHITE + "0/0");
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack createMobIcon(UUID id, String key) {
        boolean discovered = manager.hasDiscovered(id, key);
        ItemStack item;
        ItemMeta meta;
        if (discovered) {
            item = new ItemStack(Material.SKELETON_SKULL);
            meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + key);
                meta.setLore(List.of(ChatColor.GRAY + "Kills: " + ChatColor.WHITE
                        + manager.getKillCount(id, key)));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
        } else {
            item = GuiUtil.getNexoItem("lock", ChatColor.DARK_GRAY + "???");
        }
        return item;
    }

    private void openEssenceGUI(Player player, String mob) {
        List<MobEssence> essences = manager.getEssences(player.getUniqueId(), mob);
        int size = Math.max(9, ((essences.size() - 1) / 9 + 1) * 9);
        Inventory inv = Bukkit.createInventory(null, size, ESSENCE_TITLE + mob);
        GuiUtil.fillBorder(inv, filler);
        int idx = 0;
        for (MobEssence es : essences) {
            ItemStack item = new ItemStack(Material.BLAZE_POWDER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Lv " + es.level);
                List<String> lore = new ArrayList<>();
                if (es.hpMult != null) lore.add(ChatColor.GRAY + "HP x" + es.hpMult);
                if (es.damageMult != null) lore.add(ChatColor.GRAY + "DMG x" + es.damageMult);
                if (es.moveMult != null) lore.add(ChatColor.GRAY + "Move x" + es.moveMult);
                if (es.attackMult != null) lore.add(ChatColor.GRAY + "Atk x" + es.attackMult);
                if (es.countMult != null) lore.add(ChatColor.GRAY + "Count x" + es.countMult);
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            inv.setItem(idx++, item);
        }
        inv.setItem(size - 1, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title.equals(TITLE)) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (!name.equals("???")) {
                openEssenceGUI((Player) e.getWhoClicked(), name);
            }
        } else if (title.startsWith(ESSENCE_TITLE)) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (name.equalsIgnoreCase("Back")) {
                    open((Player) e.getWhoClicked());
                }
            }
        }
    }
}

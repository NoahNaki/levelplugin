package me.nakilex.levelplugin.environment;

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

import java.util.Arrays;

/** GUI for investing materials into settlement upgrades. */
public class UpgradeGUI implements Listener {
    private static final String TITLE = "Settlement Upgrade";
    private final EnvironmentManager manager;

    public UpgradeGUI(EnvironmentManager manager) {
        this.manager = manager;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(13, createItem(Material.OAK_LOG,
                ChatColor.GREEN + "Invest 1 Oak Log",
                ChatColor.GRAY + "Click to invest towards",
                ChatColor.GRAY + "the next upgrade."));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (e.getSlot() != 13) return;
        Player p = (Player) e.getWhoClicked();
        if (p.getInventory().contains(Material.OAK_LOG)) {
            p.getInventory().removeItem(new ItemStack(Material.OAK_LOG, 1));
            manager.invest(p, 1);
            open(p);
        } else {
            p.sendMessage(ChatColor.RED + "You need an oak log to invest!");
        }
    }
}

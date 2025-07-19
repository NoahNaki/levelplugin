package me.nakilex.levelplugin.environment;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** GUI for investing materials into a specific building upgrade. */
public class BuildingUpgradeGUI implements Listener {
    private static final String TITLE_PREFIX = ChatColor.BLACK + "Upgrade ";
    private final EnvironmentManager manager;
    private final Map<UUID, String> open = new HashMap<>();

    public BuildingUpgradeGUI(EnvironmentManager manager) {
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

    public void open(Player p, String building) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + building);
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(13, createItem(Material.OAK_LOG,
                ChatColor.GREEN + "Invest 1 Oak Log",
                ChatColor.GRAY + "Click to invest towards",
                ChatColor.GRAY + "the next upgrade."));
        open.put(p.getUniqueId(), building.toLowerCase());
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;
        e.setCancelled(true);
        UUID id = e.getWhoClicked().getUniqueId();
        String building = open.get(id);
        if (building == null) return;
        if (e.getSlot() != 13) return;
        Player p = (Player) e.getWhoClicked();
        if (p.getInventory().contains(Material.OAK_LOG)) {
            p.getInventory().removeItem(new ItemStack(Material.OAK_LOG, 1));
            manager.investBuilding(p, building, 1);
            open(p, building);
        } else {
            p.sendMessage(ChatColor.RED + "You need an oak log to invest!");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(TITLE_PREFIX)) {
            open.remove(e.getPlayer().getUniqueId());
        }
    }
}

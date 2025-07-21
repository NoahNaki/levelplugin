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
        me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                "[BuildingUpgradeGUI] open player=" + p.getName() + " building=" + building);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + building);
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        int stage = manager.getPlayerBuildingStage(p, building);
        var nextData = manager.getBuildingStageManager().getStage(building, stage + 1);
        java.util.List<String> lore = new java.util.ArrayList<>();
        if (nextData != null) {
            lore.add(ChatColor.GRAY + "Upgrade cost:");
            for (var e : nextData.materialCost.entrySet()) {
                String matName = EnvironmentManager.beautifyWords(e.getKey().name().toLowerCase().replace('_', ' '));
                lore.add(ChatColor.WHITE + e.getValue() + " " + matName);
            }
            lore.add(ChatColor.WHITE + "" + nextData.coinCost + " coins");
        }
        lore.add(ChatColor.GREEN + "Click to upgrade");
        inv.setItem(13, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Upgrade", lore.toArray(new String[0])));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                    "[BuildingUpgradeGUI] ignore title=" + title);
            return;
        }
        e.setCancelled(true);
        String building = title.substring(TITLE_PREFIX.length()).toLowerCase();
        me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                "[BuildingUpgradeGUI] click rawSlot=" + e.getRawSlot() +
                        " building=" + building +
                        " player=" + e.getWhoClicked().getName());
        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                    "[BuildingUpgradeGUI] ignore click in player inventory");
            return; // ignore player inventory clicks
        }
        if (e.getRawSlot() != 13) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                    "[BuildingUpgradeGUI] ignore slot=" + e.getRawSlot());
            return; // only react to our GUI slot
        }
        Player p = (Player) e.getWhoClicked();
        manager.attemptUpgradeBuilding(p, building);
        open(p, building);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(TITLE_PREFIX)) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                    "[BuildingUpgradeGUI] closed by " + e.getPlayer().getName());
        }
    }
}

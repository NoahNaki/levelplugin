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
import me.nakilex.levelplugin.utils.GuiUtil;

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
        String nice = EnvironmentManager.beautifyWords(building.replace('_', ' '));
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + nice);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        int stage = manager.getPlayerBuildingStage(p, building);
        var nextData = manager.getBuildingStageManager().getStage(building, stage + 1);
        java.util.List<String> lore = new java.util.ArrayList<>();
        if (nextData != null) {
            lore.add(ChatColor.GRAY + "Upgrade cost:");
            int coins = me.nakilex.levelplugin.Main.getInstance().getEconomyManager().getBalance(p);
            for (var e : nextData.materialCost.entrySet()) {
                org.bukkit.Material mat = e.getKey();
                int amt = e.getValue();
                boolean has = p.getInventory().containsAtLeast(new ItemStack(mat, amt), amt);
                String matName = EnvironmentManager.beautifyWords(mat.name().toLowerCase().replace('_', ' '));
                String prefix = has ? ChatColor.GREEN + "\u2714" : ChatColor.RED + "\u2718";
                String line = prefix + ChatColor.GRAY + " - "
                        + ChatColor.WHITE + amt + ChatColor.DARK_GRAY + "x "
                        + ChatColor.WHITE + matName;
                lore.add(line);
            }
            boolean hasCoins = coins >= nextData.coinCost;
            String prefix = hasCoins ? ChatColor.GREEN + "\u2714" : ChatColor.RED + "\u2718";
            String coinLine = prefix + ChatColor.GRAY + " - "
                    + ChatColor.WHITE + nextData.coinCost + " coins "
                    + ChatColor.GOLD + " <glyph:coins_icon>";
            lore.add(coinLine);
        }
        ItemStack confirm = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm");
        ItemMeta cm = confirm.getItemMeta();
        if (cm != null && !lore.isEmpty()) {
            cm.setLore(lore);
            confirm.setItemMeta(cm);
        }
        inv.setItem(11, confirm);
        inv.setItem(15, GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
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
        String building = title.substring(TITLE_PREFIX.length()).toLowerCase().replace(' ', '_');
        me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                "[BuildingUpgradeGUI] click rawSlot=" + e.getRawSlot() +
                        " building=" + building +
                        " player=" + e.getWhoClicked().getName());
        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                    "[BuildingUpgradeGUI] ignore click in player inventory");
            return; // ignore player inventory clicks
        }
        int slot = e.getRawSlot();
        if (slot != 11 && slot != 15) {
            me.nakilex.levelplugin.Main.getInstance().getLogger().info(
                    "[BuildingUpgradeGUI] ignore slot=" + slot);
            return; // only react to confirm/cancel slots
        }
        Player p = (Player) e.getWhoClicked();
        if (slot == 11) {
            manager.attemptUpgradeBuilding(p, building);
            open(p, building);
        } else {
            p.closeInventory();
        }
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

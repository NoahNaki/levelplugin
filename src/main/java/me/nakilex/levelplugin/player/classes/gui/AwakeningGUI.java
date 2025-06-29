package me.nakilex.levelplugin.player.classes.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AwakeningGUI implements Listener {
    private static final Map<UUID, Integer> stageMap = new HashMap<>();
    private static final Map<UUID, PlayerClass[]> optionMap = new HashMap<>();

    public static void open(Player player, int stage, PlayerClass left, PlayerClass right) {
        PlayerClass current = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Choose Path");
        inv.setItem(11, createItem(left != null ? Material.BOOK : Material.BARRIER,
                ChatColor.RED + (left != null ? left.name() : "Default")));
        inv.setItem(13, createItem(Material.IRON_SWORD, ChatColor.GRAY + "Stay " + current.name()));
        inv.setItem(15, createItem(right != null ? Material.BOOK : Material.BARRIER,
                ChatColor.AQUA + (right != null ? right.name() : "Default")));
        player.openInventory(inv);
        stageMap.put(player.getUniqueId(), stage);
        optionMap.put(player.getUniqueId(), new PlayerClass[]{left, right});
    }

    private static ItemStack createItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(e.getView().getTitle()).equalsIgnoreCase("Choose Path")) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        UUID uuid = player.getUniqueId();
        int stage = stageMap.getOrDefault(uuid, 0);
        PlayerClass[] opts = optionMap.get(uuid);
        if (opts == null) return;
        PlayerClass current = StatsManager.getInstance().getPlayerStats(uuid).playerClass;
        PlayerClass newClass = current;
        if (e.getSlot() == 11 && opts[0] != null) newClass = opts[0];
        else if (e.getSlot() == 15 && opts[1] != null) newClass = opts[1];
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(uuid);
        ps.playerClass = newClass;
        ps.awakeningStage = Math.max(ps.awakeningStage, stage);
        ps.unlockedClasses.add(newClass);
        // Unlock the other subclass choice for later switching
        if (opts[0] != null) ps.unlockedClasses.add(opts[0]);
        if (opts[1] != null) ps.unlockedClasses.add(opts[1]);
        player.sendMessage(ChatColor.GREEN + "Class changed to " + newClass.name());
        player.closeInventory();
        stageMap.remove(uuid);
        optionMap.remove(uuid);
    }
}

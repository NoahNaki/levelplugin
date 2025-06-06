package me.nakilex.levelplugin.blacksmith.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class RerollBrowser implements CommandExecutor, Listener {

    private static final int SIZE = 27;
    private static final String TITLE = ChatColor.GRAY + "Reroll Items";

    private final JavaPlugin plugin;

    public RerollBrowser(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("rerollbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static ItemStack menuItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);

        ItemStack filler = menuItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) gui.setItem(i, filler);

        gui.setItem(10, menuItem(Material.BORDURE_INDENTED_BANNER_PATTERN, ChatColor.GREEN + "STR Placeholder"));
        gui.setItem(11, menuItem(Material.FLOWER_BANNER_PATTERN, ChatColor.GREEN + "INT Placeholder"));
        gui.setItem(12, menuItem(Material.FLOW_BANNER_PATTERN, ChatColor.GREEN + "AGI Placeholder"));
        gui.setItem(14, menuItem(Material.SKULL_BANNER_PATTERN, ChatColor.GREEN + "HEALTH Placeholder"));
        gui.setItem(15, menuItem(Material.GUSTER_BANNER_PATTERN, ChatColor.GREEN + "DEX Placeholder"));
        gui.setItem(16, menuItem(Material.GLOBE_BANNER_PATTERN, ChatColor.GREEN + "DEF Placeholder"));

        player.openInventory(gui);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        openGui(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        player.getInventory().addItem(clicked.clone());
        player.sendMessage(ChatColor.GREEN + "You received: " + clicked.getItemMeta().getDisplayName());
    }
}

package me.nakilex.levelplugin.items.tools.gui;

import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ToolBrowser implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private static final String TITLE = "Tools Browser";

    public ToolBrowser(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("toolsbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createToolItem(Player viewer, CustomTool tool) {
        return ToolManager.getInstance().createToolItem(tool, viewer);
    }

    private void open(Player player) {
        List<CustomTool> mining = ToolManager.getInstance().getTools(ToolDiscipline.MINING);
        List<CustomTool> farming = ToolManager.getInstance().getTools(ToolDiscipline.FARMING);
        List<CustomTool> fishing = ToolManager.getInstance().getTools(ToolDiscipline.FISHING);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        fillRow(inv, 0, mining, player);
        fillRow(inv, 9, farming, player);
        fillRow(inv, 18, fishing, player);
        player.openInventory(inv);
    }

    private void fillRow(Inventory inv, int start, List<CustomTool> tools, Player player) {
        for (int i = 0; i < tools.size() && i < 9; i++) {
            inv.setItem(start + i, createToolItem(player, tools.get(i)));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        open(p);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        player.getInventory().addItem(clicked.clone());
        player.sendMessage(ChatColor.GREEN + "You received a " + clicked.getItemMeta().getDisplayName());
    }
}

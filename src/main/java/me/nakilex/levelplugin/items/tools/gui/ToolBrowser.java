package me.nakilex.levelplugin.items.tools.gui;

import me.nakilex.levelplugin.items.tools.CustomTool;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ToolBrowser implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private static final String TITLE = ChatColor.GRAY + "Tools Browser";

    public ToolBrowser(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("toolsbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createToolItem(Player viewer, CustomTool tool) {
        ItemStack it = new ItemStack(tool.getMaterial());
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            ToolTier tier = tool.getTier();
            ChatColor color = tier.getRarity().getColor();
            meta.setDisplayName(color + tool.getName());

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            int req = tier.getLevelRequirement();
            int lvl = me.nakilex.levelplugin.player.mining.managers.MiningManager.getInstance().getLevel(viewer);
            String symbol = lvl >= req ? "§a✔ " : "§c✘ ";
            lore.add(symbol + ChatColor.GRAY + "Mining Lv. Requirement: " + ChatColor.WHITE + req);
            lore.add(ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + tier.getMiningSpeed());
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            it.setItemMeta(meta);
        }
        return it;
    }

    private void open(Player player) {
        List<CustomTool> tools = ToolManager.getInstance().getTools();
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);
        for (int i = 0; i < tools.size() && i < 9; i++) {
            inv.setItem(i, createToolItem(player, tools.get(i)));
        }
        player.openInventory(inv);
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

package me.nakilex.levelplugin.items.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
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

public class ItemsBrowser implements CommandExecutor, Listener {
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int PAGE_SIZE = 28; // 4 rows × 7 cols of content

    private static final int TYPE_FILTER_SLOT   = 46;
    private static final int RARITY_FILTER_SLOT = 48;
    private static final int LEVEL_FILTER_SLOT  = 50;

    private final JavaPlugin plugin;
    private final java.util.Map<java.util.UUID,Integer> typeFilters   = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID,Integer> rarityFilters = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID,Integer> levelFilters  = new java.util.HashMap<>();

    public ItemsBrowser(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("itemsbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title(int page) {
        return ChatColor.BLACK + "Items Browser - Page " + (page + 1);
    }

    private static ItemStack createMenuItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getNexoItem(String id, String name) {
        com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(id);
        if (b == null) return new ItemStack(Material.BARRIER);
        ItemStack it = b.build();
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }

    private ItemStack createTypeButton(int filter) {
        ItemStack it = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Type Filter");
            List<String> lore = new ArrayList<>();
            String[] types = {"ARMOR","WEAPON","ALL"};
            for (int i = 0; i < types.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + types[i];
                lore.add(line);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createRarityButton(int filter) {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Rarity Filter");
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            me.nakilex.levelplugin.items.data.ItemRarity[] arr = me.nakilex.levelplugin.items.data.ItemRarity.values();
            for (int i = 0; i < arr.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + arr[i].name();
                lore.add(line);
            }
            lore.add((arr.length == filter ? ChatColor.GREEN : ChatColor.GRAY) + "ALL");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createLevelButton(int filter) {
        ItemStack it = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Level Filter");
            List<String> lore = new ArrayList<>();
            String[] ranges = {"Lv. 1-19","Lv. 20-39","Lv. 40-59","Lv. 60-79","Lv. 80+","ALL"};
            for (int i = 0; i < ranges.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + ranges[i];
                lore.add(line);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }


    private void openPage(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, SIZE, title(page));

        ItemStack filler = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, filler);
        }

        ItemStack info = createMenuItem(Material.BARRIER, ChatColor.RED + "Legacy Items Disabled",
                ChatColor.GRAY + "Item templates from items.yml",
                ChatColor.GRAY + "are no longer supported.");
        gui.setItem(22, info);

        player.openInventory(gui);
    }





    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can browse items.");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Legacy items browser is disabled.");
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!ChatColor.stripColor(e.getView().getTitle()).startsWith("Items Browser")) return;
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        player.sendMessage(ChatColor.RED + "Legacy items browser is disabled.");
    }
}

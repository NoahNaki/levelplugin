package me.nakilex.levelplugin.enchanting.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EnchantingGUI implements Listener {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Enchant Item";
    private static final int SIZE = 27;

    private final ItemManager itemManager;
    private final Map<StatType, String> prefixes = new HashMap<>();
    private final Map<Integer, StatType> slotMap = new HashMap<>();

    public EnchantingGUI(Main plugin, ItemManager itemManager) {
        this.itemManager = itemManager;
        File file = new File(plugin.getDataFolder(), "prefixes.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        prefixes.put(StatType.HP, cfg.getString("hp", "Hearty"));
        prefixes.put(StatType.DEF, cfg.getString("defense", "Sturdy"));
        prefixes.put(StatType.STR, cfg.getString("strength", "Mighty"));
        prefixes.put(StatType.AGI, cfg.getString("agility", "Nimble"));
        prefixes.put(StatType.INT, cfg.getString("intelligence", "Wise"));
        prefixes.put(StatType.DEX, cfg.getString("dexterity", "Precise"));

        // Slot setup for buttons
        slotMap.put(10, StatType.HP);
        slotMap.put(11, StatType.DEF);
        slotMap.put(12, StatType.STR);
        slotMap.put(14, StatType.AGI);
        slotMap.put(15, StatType.INT);
        slotMap.put(16, StatType.DEX);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, SIZE, TITLE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler());
        inv.setItem(13, null);

        inv.setItem(10, createButton(Material.RED_DYE, prefixes.get(StatType.HP)));
        inv.setItem(11, createButton(Material.SHIELD, prefixes.get(StatType.DEF)));
        inv.setItem(12, createButton(Material.IRON_SWORD, prefixes.get(StatType.STR)));
        inv.setItem(14, createButton(Material.FEATHER, prefixes.get(StatType.AGI)));
        inv.setItem(15, createButton(Material.BOOK, prefixes.get(StatType.INT)));
        inv.setItem(16, createButton(Material.ARROW, prefixes.get(StatType.DEX)));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        int slot = e.getRawSlot();
        if (slot == 13) return; // allow placing item
        e.setCancelled(true);

        StatType type = slotMap.get(slot);
        if (type == null) return;

        Inventory inv = e.getInventory();
        ItemStack stack = inv.getItem(13);
        Player player = (Player) e.getWhoClicked();
        if (stack == null || stack.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Place an item in the center first.");
            return;
        }

        CustomItem cItem = itemManager.getCustomItemFromItemStack(stack);
        if (cItem == null) {
            player.sendMessage(ChatColor.RED + "That item cannot be enchanted.");
            return;
        }

        // Prevent double prefixes
        Set<String> all = Set.copyOf(prefixes.values());
        for (String pre : all) {
            if (cItem.getBaseName().startsWith(pre + " ")) {
                player.sendMessage(ChatColor.RED + "Item already has a prefix.");
                return;
            }
        }

        applyPrefix(player, stack, cItem, type);
        player.closeInventory();
    }

    private void applyPrefix(Player player, ItemStack stack, CustomItem item, StatType type) {
        String prefix = prefixes.get(type);
        item.setBaseName(prefix + " " + item.getBaseName());
        int bonus = 20;
        switch (type) {
            case HP -> item.setBaseHp(item.getHp() + bonus);
            case DEF -> item.setBaseDef(item.getDef() + bonus);
            case STR -> item.setBaseStr(item.getStr() + bonus);
            case AGI -> item.setBaseAgi(item.getAgi() + bonus);
            case INT -> item.setBaseIntel(item.getIntel() + bonus);
            case DEX -> item.setBaseDex(item.getDex() + bonus);
        }
        ItemStack updated = ItemUtil.createItemStackFromCustomItem(item, stack.getAmount(), player);
        stack.setType(updated.getType());
        stack.setItemMeta(updated.getItemMeta());
        player.sendMessage(ChatColor.GREEN + "Enchanted with " + prefix + "!");
    }
}

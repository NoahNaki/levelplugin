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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class EnchantingGUI implements Listener {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Enchant Item";
    private static final int SIZE = 27;

    private final ItemManager itemManager;
    private final Map<StatType, String> prefixes = new HashMap<>();
    private final java.util.List<StatType> prefixList = new java.util.ArrayList<>();
    private final Map<Player, Inventory> openInventories = new HashMap<>();

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

        prefixList.addAll(prefixes.keySet());
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
        inv.setItem(22, createEnchantButton(false));
        inv.setItem(8, createInfoItem());

        openInventories.put(player, inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        Player player = (Player) e.getWhoClicked();
        Inventory gui = openInventories.get(player);
        if (gui == null || !gui.equals(e.getView().getTopInventory())) return;

        int raw = e.getRawSlot();

        // bottom inventory actions allowed; update after shift-click
        if (raw >= gui.getSize()) {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> updateButton(player, gui), 1L);
            }
            return;
        }

        // allow place/remove in slot 13
        if (raw == 13) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> updateButton(player, gui), 1L);
            return;
        }

        e.setCancelled(true);

        if (raw != 22) return;

        ItemStack stack = gui.getItem(13);
        if (stack == null || stack.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Place an item in the center first.");
            return;
        }

        CustomItem cItem = itemManager.getCustomItemFromItemStack(stack);
        if (cItem == null) {
            player.sendMessage(ChatColor.RED + "That item cannot be enchanted.");
            return;
        }

        for (String pre : prefixes.values()) {
            if (cItem.getBaseName().startsWith(pre + " ")) {
                player.sendMessage(ChatColor.RED + "Item already has a prefix.");
                return;
            }
        }

        applyRandomPrefix(player, stack, cItem);
        updateButton(player, gui);
    }

    private ItemStack createEnchantButton(boolean ready) {
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (ready) {
                meta.setDisplayName(ChatColor.GREEN + "Apply Random Prefix");
                meta.setLore(java.util.List.of(
                        ChatColor.GRAY + "Adds +20 to a random stat",
                        ChatColor.GRAY + "and appends a prefix to the name."));
            } else {
                meta.setDisplayName(ChatColor.GRAY + "Place an item");
                meta.setLore(java.util.List.of(ChatColor.DARK_GRAY + "Insert an item in the center."));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Information");
            meta.setLore(java.util.List.of(
                    ChatColor.GRAY + "Place an item in the center slot",
                    ChatColor.GRAY + "Click the enchant table to roll",
                    ChatColor.GRAY + "a random stat prefix."));
            info.setItemMeta(meta);
        }
        return info;
    }

    private void applyRandomPrefix(Player player, ItemStack stack, CustomItem item) {
        StatType type = prefixList.get(ThreadLocalRandom.current().nextInt(prefixList.size()));
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
        player.sendMessage(ChatColor.GREEN + "Item enchanted with " + prefix + "!");
    }

    private void updateButton(Player player, Inventory gui) {
        ItemStack current = gui.getItem(13);
        boolean ready = false;
        if (current != null && !current.getType().isAir()) {
            CustomItem ci = itemManager.getCustomItemFromItemStack(current);
            if (ci != null) {
                ready = prefixes.values().stream().noneMatch(p -> ci.getBaseName().startsWith(p + " "));
            }
        }
        gui.setItem(22, createEnchantButton(ready));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory gui = openInventories.remove(e.getPlayer());
        if (gui == null || !gui.equals(e.getInventory())) return;
        ItemStack center = gui.getItem(13);
        if (center != null && !center.getType().isAir()) {
            e.getPlayer().getInventory().addItem(center);
        }
    }
}

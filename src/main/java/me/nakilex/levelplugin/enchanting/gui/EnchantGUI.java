package me.nakilex.levelplugin.enchanting.gui;

import me.nakilex.levelplugin.enchanting.managers.EnchantManager;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.Main;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EnchantGUI implements Listener {
    private static final int SIZE = 27;
    private static final int INFO_SLOT = 8;
    private static final String TITLE = ChatColor.DARK_PURPLE + "Enchant";

    private final EnchantManager manager;
    private final EconomyManager economy;
    private final Map<UUID, Inventory> open = new HashMap<>();

    public EnchantGUI(EnchantManager manager, EconomyManager economy) {
        this.manager = manager;
        this.economy = economy;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(player, SIZE, TITLE);
        ItemStack filler = createFiller();
        for (int i = 0; i < SIZE; i++) gui.setItem(i, filler);
        gui.setItem(INFO_SLOT, createInfoItem());
        gui.setItem(13, null);
        gui.setItem(22, createButton(0));
        open.put(player.getUniqueId(), gui);
        player.openInventory(gui);
    }

    private ItemStack createFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); pane.setItemMeta(meta); }
        return pane;
    }

    private ItemStack createInfoItem() {
        ItemBuilder builder = NexoItems.getItemById("info");
        ItemStack item = builder == null ? new ItemStack(Material.BOOK) : builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Information");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Place a custom item in the center.",
                    ChatColor.GRAY + "Click " + ChatColor.LIGHT_PURPLE + "Enchant" + ChatColor.GRAY + " to add",
                    ChatColor.GRAY + "a random prefix giving +20 to one stat.",
                    ChatColor.GRAY + "Cost doubles every enchant.")
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(int cost) {
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchant");
        meta.setLore(Collections.singletonList(cost > 0
                ? ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "⛃ " + cost
                : ChatColor.GRAY + "Place item to enchant"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Inventory gui = open.get(p.getUniqueId());
        if (gui == null || !e.getView().getTopInventory().equals(gui)) return;
        int rawSlot = e.getRawSlot();
        if (rawSlot == 13) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> update(p, gui), 1L);
            return;
        }

        // Allow shift-clicking items from player inventory into slot 13
        if (rawSlot >= gui.getSize()) {
            if (e.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && (gui.getItem(13) == null || gui.getItem(13).getType().isAir())) {
                e.setCancelled(false);
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> update(p, gui), 1L);
            }
            return;
        }

        e.setCancelled(true);
        if (rawSlot == 22) {
            ItemStack item = gui.getItem(13);
            if (item == null || item.getType().isAir()) return;
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(item);
            if (ci == null) return;
            int cost = manager.getEnchantCost(ci);
            try {
                economy.deductCoins(p, cost);
            } catch (IllegalArgumentException ex) {
                p.sendMessage(ChatColor.RED + "Not enough coins! Cost: " + cost);
                return;
            }
            String prefix = manager.enchant(p, item, ci);
            gui.setItem(13, item);
            ItemUtil.updateCustomItemTooltip(item, p);
            p.sendMessage(ChatColor.GREEN + "Item enchanted with " + ChatColor.LIGHT_PURPLE + prefix + ChatColor.GREEN + "!");
            update(p, gui);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory gui = open.get(e.getPlayer().getUniqueId());
        if (gui == null || !e.getInventory().equals(gui)) return;
        ItemStack it = gui.getItem(13);
        if (it != null && !it.getType().isAir()) {
            ((Player)e.getPlayer()).getInventory().addItem(it);
        }
        open.remove(e.getPlayer().getUniqueId());
    }

    private void update(Player p, Inventory gui) {
        ItemStack stack = gui.getItem(13);
        if (stack == null || stack.getType().isAir()) {
            gui.setItem(22, createButton(0));
            return;
        }
        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (ci == null) {
            gui.setItem(22, createButton(0));
            return;
        }
        gui.setItem(22, createButton(manager.getEnchantCost(ci)));
    }
}

package me.nakilex.levelplugin.auctionhouse;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseGUI implements Listener {
    private static final String TITLE = ChatColor.GOLD + "Auction House";
    private static final int SIZE = 54;
    private final AuctionHouseManager manager;
    private final EconomyManager economy;
    private final NamespacedKey indexKey;

    public AuctionHouseGUI(JavaPlugin plugin, AuctionHouseManager manager, EconomyManager economy) {
        this.manager = manager;
        this.economy = economy;
        this.indexKey = new NamespacedKey(plugin, "auction_index");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = createFiller();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        List<AuctionItem> list = manager.getAuctions();
        int slot = 0;
        for (int i = 0; i < list.size() && slot < 45; i++) {
            AuctionItem ai = list.get(i);
            ItemStack stack = ai.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.YELLOW + "Price: " + ai.getPrice() + " coins");
                lore.add(ChatColor.GRAY + "Click to buy");
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(indexKey, PersistentDataType.INTEGER, i);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot++, stack);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        Integer idx = clicked.getItemMeta().getPersistentDataContainer().get(indexKey, PersistentDataType.INTEGER);
        if (idx == null) return;
        Player player = (Player) e.getWhoClicked();
        manager.purchase(player, idx);
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), () -> open(player), 1L);
    }

    private ItemStack createFiller() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            it.setItemMeta(meta);
        }
        return it;
    }
}

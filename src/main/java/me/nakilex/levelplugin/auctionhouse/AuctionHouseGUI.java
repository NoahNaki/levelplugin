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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AuctionHouseGUI implements Listener {
    private static final String TITLE = ChatColor.GOLD + "Auction House";
    private static final int SIZE = 54;
    private static final int SELL_SLOT = 49;

    private final JavaPlugin plugin;
    private final AuctionHouseManager manager;
    private final EconomyManager economy;
    private final NamespacedKey indexKey;

    private final Map<UUID, ItemStack> pendingListings = new HashMap<>();

    public AuctionHouseGUI(JavaPlugin plugin, AuctionHouseManager manager, EconomyManager economy) {
        this.plugin = plugin;
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

        inv.setItem(SELL_SLOT, createSellButton());

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        Player player = (Player) e.getWhoClicked();

        if (e.getRawSlot() == SELL_SLOT) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
                return;
            }
            pendingListings.put(player.getUniqueId(), hand.clone());
            player.getInventory().setItemInMainHand(null);
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type the price in chat or 'cancel' to abort.");
            return;
        }

        Integer idx = clicked.getItemMeta().getPersistentDataContainer().get(indexKey, PersistentDataType.INTEGER);
        if (idx == null) return;
        manager.purchase(player, idx);
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 1L);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (!pendingListings.containsKey(id)) return;
        e.setCancelled(true);
        String msg = e.getMessage();
        if (msg.equalsIgnoreCase("cancel")) {
            ItemStack item = pendingListings.remove(id);
            Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().getInventory().addItem(item));
            e.getPlayer().sendMessage(ChatColor.RED + "Listing cancelled.");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(msg);
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid number. Enter a price or 'cancel'.");
            return;
        }
        ItemStack item = pendingListings.remove(id);
        Bukkit.getScheduler().runTask(plugin, () -> {
            manager.listItem(e.getPlayer(), item, price);
            e.getPlayer().sendMessage(ChatColor.GREEN + "Item listed for " + price + " coins.");
        });
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

    private ItemStack createSellButton() {
        ItemStack it = new ItemStack(Material.EMERALD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "List Item (hand)");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Hold an item in your hand and click.");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }
}

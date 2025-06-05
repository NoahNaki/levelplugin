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
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;

    private final JavaPlugin plugin;
    private final AuctionHouseManager manager;
    private final EconomyManager economy;
    private final NamespacedKey indexKey;

    private static class ListingData {
        ItemStack item;
        int step = 0;
        int start;
        int bin;
        long duration;
    }

    private final Map<UUID, ListingData> pending = new HashMap<>();
    private final Map<UUID, Integer> pageMap = new HashMap<>();

    public AuctionHouseGUI(JavaPlugin plugin, AuctionHouseManager manager, EconomyManager economy) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
        this.indexKey = new NamespacedKey(plugin, "auction_index");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = createFiller();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        List<AuctionItem> list = manager.getAuctions();
        int startIndex = page * 45;
        int slot = 0;
        for (int i = startIndex; i < list.size() && slot < 45; i++) {
            AuctionItem ai = list.get(i);
            ItemStack stack = ai.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Start: " + ai.getStartingPrice());
                if (ai.getCurrentBid() > 0) {
                    lore.add(ChatColor.AQUA + "Current bid: " + ai.getCurrentBid());
                }
                if (ai.getBinPrice() > 0) {
                    lore.add(ChatColor.GREEN + "BIN: " + ai.getBinPrice());
                }
                long left = (ai.getEndTime() - System.currentTimeMillis()) / 1000;
                long mins = left / 60;
                lore.add(ChatColor.GRAY + "Time left: " + mins + "m");
                lore.add(ChatColor.GRAY + "Category: " + ai.getCategory().name());
                lore.add(ChatColor.GRAY + "Click to buy (BIN) or /ah bid " + i + " <amount>");
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(indexKey, PersistentDataType.INTEGER, i);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot++, stack);
        }

        if (page > 0) inv.setItem(PREV_PAGE, createArrow(ChatColor.RED + "Previous"));
        if (list.size() > (page + 1) * 45) inv.setItem(NEXT_PAGE, createArrow(ChatColor.GREEN + "Next"));
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

        int rawSlot = e.getRawSlot();
        if (rawSlot == SELL_SLOT) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold the item you wish to sell in your hand.");
                return;
            }
            ListingData data = new ListingData();
            data.item = hand.clone();
            pending.put(player.getUniqueId(), data);
            player.getInventory().setItemInMainHand(null);
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Enter starting price or 'cancel'.");
            return;
        }

        if (rawSlot == NEXT_PAGE) {
            int page = pageMap.getOrDefault(player.getUniqueId(), 0) + 1;
            open(player, page);
            return;
        }

        if (rawSlot == PREV_PAGE) {
            int page = Math.max(0, pageMap.getOrDefault(player.getUniqueId(), 0) - 1);
            open(player, page);
            return;
        }

        Integer idx = clicked.getItemMeta().getPersistentDataContainer().get(indexKey, PersistentDataType.INTEGER);
        if (idx == null) return;
        AuctionItem ai = manager.getAuctions().get(idx);
        if (ai.getBinPrice() > 0) {
            manager.buyNow(player, idx);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, pageMap.getOrDefault(player.getUniqueId(), 0)), 1L);
        } else {
            player.sendMessage(ChatColor.YELLOW + "Use /auctionhouse bid " + idx + " <amount> to bid.");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ListingData data = pending.get(id);
        if (data == null) return;
        e.setCancelled(true);
        String msg = e.getMessage();
        if (msg.equalsIgnoreCase("cancel")) {
            ItemStack item = data.item;
            pending.remove(id);
            Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().getInventory().addItem(item));
            e.getPlayer().sendMessage(ChatColor.RED + "Listing cancelled.");
            return;
        }
        try {
            switch (data.step) {
                case 0 -> {
                    data.start = Integer.parseInt(msg);
                    data.step = 1;
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "Enter BIN price or 0.");
                }
                case 1 -> {
                    data.bin = Integer.parseInt(msg);
                    data.step = 2;
                    e.getPlayer().sendMessage(ChatColor.YELLOW + "Enter duration in hours (e.g. 6)");
                }
                case 2 -> {
                    data.duration = Long.parseLong(msg);
                    ItemStack item = data.item;
                    pending.remove(id);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        manager.listItem(e.getPlayer(), item, data.start, data.bin, data.duration);
                        e.getPlayer().sendMessage(ChatColor.GREEN + "Item listed.");
                    });
                }
            }
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid number. Type again or 'cancel'.");
        }
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

    private ItemStack createArrow(String name) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }
}

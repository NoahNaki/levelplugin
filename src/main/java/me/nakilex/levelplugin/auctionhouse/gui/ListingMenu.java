package me.nakilex.levelplugin.auctionhouse.gui;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import me.nakilex.levelplugin.auctionhouse.gui.PriceInputPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ListingMenu {
    public static final String TITLE = ChatColor.DARK_GREEN + "List Item";

    private static class Session {
        ItemStack item;
        double price = 1.0;
    }

    private final AuctionHouseManager manager;
    private final ConversationFactory conversationFactory;
    private final java.util.Map<java.util.UUID, Session> sessions = new java.util.HashMap<>();

    public ListingMenu(AuctionHouseManager manager) {
        this.manager = manager;
        this.conversationFactory = new ConversationFactory(manager.getPlugin())
                .withLocalEcho(false)
                .withTimeout(30);
    }

    public void open(Player player) {
        Session s = sessions.computeIfAbsent(player.getUniqueId(), k -> new Session());
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        gui.setItem(11, s.item == null ? new ItemStack(Material.AIR) : s.item);
        gui.setItem(15, createPriceItem(s.price));
        gui.setItem(18, getOraxenItem("cross", ChatColor.RED + "Cancel"));
        gui.setItem(26, getOraxenItem("check", ChatColor.GREEN + "Confirm"));
        player.openInventory(gui);
    }

    private ItemStack createPriceItem(double price) {
        ItemStack i = new ItemStack(Material.PAPER);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.YELLOW + "Set Price: " + price);
            m.setLore(java.util.List.of(ChatColor.GRAY + "Click to enter amount"));
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack getOraxenItem(String id, String name) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(Player player, int slot) {
        Session s = sessions.computeIfAbsent(player.getUniqueId(), k -> new Session());
        if (slot == 11) {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                s.item = cursor.clone();
                player.setItemOnCursor(null);
                open(player);
            }
        } else if (slot == 15) {
            openPriceChatInput(player);
        } else if (slot == 26) {
            if (s.item != null) {
                if (manager.listItem(player, s.item, s.price)) {
                    player.sendMessage(ChatColor.GREEN + "Item listed for " + s.price);
                }
                sessions.remove(player.getUniqueId());
                player.closeInventory();
            }
        } else if (slot == 18) {
            if (s.item != null) player.getInventory().addItem(s.item);
            sessions.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private void openPriceChatInput(Player player) {
        conversationFactory.withFirstPrompt(new PriceInputPrompt(this, player))
                .addConversationAbandonedListener(new ConversationAbandonedListener() {
                    @Override
                    public void conversationAbandoned(ConversationAbandonedEvent event) {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> open(player));
                    }
                })
                .buildConversation(player).begin();
        player.sendMessage(ChatColor.GRAY + "Type the price in chat or 'cancel'.");
    }

    void setPrice(Player player, double price) {
        sessions.computeIfAbsent(player.getUniqueId(), k -> new Session()).price = price;
    }

    void reopen(Player player, boolean refreshPrice) {
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> open(player));
    }
}

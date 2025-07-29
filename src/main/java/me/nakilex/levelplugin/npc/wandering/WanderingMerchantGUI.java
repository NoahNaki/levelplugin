package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
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
import org.bukkit.plugin.Plugin;

import java.util.*;

/** Simple GUI displaying random offers from the wandering merchant. */
public class WanderingMerchantGUI implements Listener {
    private final Plugin plugin;
    private final EconomyManager economy;
    private final Inventory inv;
    private final Map<Integer, WanderingMerchantOffer> offers = new HashMap<>();
    private final Set<UUID> viewers = new HashSet<>();

    public WanderingMerchantGUI(Plugin plugin, List<WanderingMerchantOffer> list) {
        this.plugin = plugin;
        this.economy = Main.getInstance().getEconomyManager();
        this.inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Wandering Merchant");
        for (int i = 0; i < list.size(); i++) {
            WanderingMerchantOffer of = list.get(i);
            offers.put(10 + i, of);
            inv.setItem(10 + i, decorate(of));
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Collection<WanderingMerchantOffer> getOffers() { return offers.values(); }

    private ItemStack decorate(WanderingMerchantOffer offer) {
        ItemStack stack = offer.getItem().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");
            if (offer.getStock() > 0) {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.YELLOW + offer.getCost() + " <glyph:coins_icon>");
                lore.add(ChatColor.GRAY + "Stock: " + offer.getStock());
            } else {
                lore.add(ChatColor.RED + "Out of stock!");
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void open(Player player) {
        viewers.add(player.getUniqueId());
        player.openInventory(inv);
    }

    public void closeAll() {
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.getOpenInventory().getTopInventory().equals(inv)) {
                p.closeInventory();
            }
        }
        viewers.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        WanderingMerchantOffer offer = offers.get(slot);
        if (offer == null) return;
        Player player = (Player) e.getWhoClicked();
        if (offer.getStock() <= 0) {
            player.sendMessage(ChatColor.RED + "Out of stock!");
            return;
        }
        if (economy.getBalance(player) < offer.getCost()) {
            player.sendMessage(ChatColor.RED + "Not enough coins!");
            return;
        }
        economy.deductCoins(player, offer.getCost());
        player.getInventory().addItem(offer.getItem());
        offer.decrement();
        inv.setItem(slot, decorate(offer));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inv)) {
            viewers.remove(e.getPlayer().getUniqueId());
        }
    }
}

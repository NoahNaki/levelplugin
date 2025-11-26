package me.nakilex.levelplugin.mercenary.gui;

import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.MercenaryGift;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple browser that lets designers self-serve friendship gifts for testing.
 */
public class MercenaryGiftBrowserGUI implements Listener {
    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.DARK_GREEN + "Mercenary Gifts";

    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;

    public MercenaryGiftBrowserGUI(Plugin plugin, MercenaryAffinityManager affinityManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        GuiBuilder builder = GuiBuilder.create(SIZE, TITLE).border();
        int slot = 10;
        for (MercenaryGift gift : affinityManager.getGifts()) {
            builder.setItem(slot, decorate(gift));
            if ((slot + 1) % 9 == 8) {
                slot += 3;
            } else {
                slot++;
            }
        }
        player.openInventory(builder.build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            player.getInventory().addItem(clicked.clone());
            player.sendMessage(ChatColor.GREEN + "Added gift to your inventory for testing.");
        }
    }

    private ItemStack decorate(MercenaryGift gift) {
        ItemStack stack = gift.getIcon();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            if (meta.getLore() != null) {
                lore.addAll(meta.getLore());
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

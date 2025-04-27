package me.nakilex.levelplugin.economy.gui;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GemExchangeGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "Gem Exchange";
    private static final Material FRAGMENT = Material.MEDIUM_AMETHYST_BUD;
    private static final Material SHARD    = Material.AMETHYST_SHARD;
    private static final Material CLUSTER  = Material.AMETHYST_CLUSTER;

    private final Main plugin;
    private final Inventory gui;

    public GemExchangeGUI(Main plugin) {
        this.plugin = plugin;
        this.gui = Bukkit.createInventory(null, 27, TITLE);
        initItems();
        fillFiller();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void initItems() {
        gui.setItem(10, createGuiItem(FRAGMENT,
            ChatColor.AQUA + "64 Fragments → 1 Shard",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Combine 64 fragments into 1 shard"
            )
        ));
        gui.setItem(12, createGuiItem(SHARD,
            ChatColor.AQUA + "1 Shard → 64 Fragments",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Break 1 shard into 64 fragments"
            )
        ));
        gui.setItem(14, createGuiItem(SHARD,
            ChatColor.AQUA + "64 Shards → 1 Cluster",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Combine 64 shards into 1 cluster"
            )
        ));
        gui.setItem(16, createGuiItem(CLUSTER,
            ChatColor.AQUA + "1 Cluster → 64 Shards",
            Arrays.asList(
                "",
                ChatColor.GRAY + "Break 1 cluster into 64 shards"
            )
        ));
    }

    private void fillFiller() {
        ItemStack filler = createFillerItem(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < gui.getSize(); slot++) {
            if (gui.getItem(slot) == null) {
                gui.setItem(slot, filler);
            }
        }
    }

    public void open(Player player) {
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent ev) {
        if (!ev.getView().getTitle().equals(TITLE)) return;
        ev.setCancelled(true);
        if (ev.getCurrentItem() == null) return;
        Player p = (Player) ev.getWhoClicked();
        switch (ev.getSlot()) {
            case 10 -> handleConvert(p, FRAGMENT, 64, SHARD, 1);
            case 12 -> handleConvert(p, SHARD, 1, FRAGMENT, 64);
            case 14 -> handleConvert(p, SHARD, 64, CLUSTER, 1);
            case 16 -> handleConvert(p, CLUSTER, 1, SHARD, 64);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent ev) {
        if (ev.getView().getTitle().equals(TITLE)) {
            // no-op
        }
    }

    private void handleConvert(Player p,
                               Material fromMat, int fromAmt,
                               Material toMat,   int toAmt) {
        Map<Integer, ItemStack> map = (Map<Integer, ItemStack>) p.getInventory().all(fromMat);
        int total = map.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (total < fromAmt) {
            p.sendMessage(ChatColor.RED + "Not enough " + fromMat.name().toLowerCase().replace('_',' ') + "! Need " + fromAmt + ".");
            return;
        }
        int rem = fromAmt;
        for (var e : map.entrySet()) {
            ItemStack stack = e.getValue();
            int amt = stack.getAmount();
            if (amt <= rem) {
                p.getInventory().clear(e.getKey());
                rem -= amt;
            } else {
                stack.setAmount(amt - rem);
                rem = 0;
            }
            if (rem == 0) break;
        }
        p.getInventory().addItem(new ItemStack(toMat, toAmt));
        p.sendMessage(ChatColor.GREEN + "Converted " + fromAmt + " " + fromMat.name().toLowerCase().replace('_',' ') + " into " + toAmt + " " + toMat.name().toLowerCase().replace('_',' ') + "!");
        new BukkitRunnable() {
            @Override public void run() {
                open(p);
            }
        }.runTaskLater(plugin, 1L);
    }

    private ItemStack createGuiItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem(Material mat) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}

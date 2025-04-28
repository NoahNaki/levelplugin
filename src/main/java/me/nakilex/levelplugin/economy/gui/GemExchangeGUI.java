package me.nakilex.levelplugin.economy.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.GemsManager;
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
    private final GemsManager gemsManager;

    public GemExchangeGUI(Main plugin, GemsManager gemsManager) {
        this.plugin = plugin;
        this.gui = Bukkit.createInventory(null, 27, TITLE);
        this.gemsManager  = gemsManager;
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
        // 1) Count how many of the “from” material the player has
        @SuppressWarnings("unchecked")
        Map<Integer, ItemStack> map = (Map<Integer, ItemStack>) p.getInventory().all(fromMat);
        int total = map.values().stream().mapToInt(ItemStack::getAmount).sum();

        // 2) If they don’t have enough, bail out
        if (total < fromAmt) {
            p.sendMessage(ChatColor.RED
                + "Not enough "
                + fromMat.name().toLowerCase().replace('_',' ')
                + "! Need " + fromAmt + ".");
            return;
        }

        // 3) Remove exactly `fromAmt` items from their inventory
        int rem = fromAmt;
        for (var entry : map.entrySet()) {
            ItemStack stack = entry.getValue();
            int amt = stack.getAmount();

            if (amt <= rem) {
                p.getInventory().clear(entry.getKey());
                rem -= amt;
            } else {
                stack.setAmount(amt - rem);
                rem = 0;
            }

            if (rem == 0) break;
        }

        // 4) Build the “pretty” custom item instead of a vanilla one
        //    Determine the unit‐value of the target material:
        int unitValue;
        if (toMat == Material.MEDIUM_AMETHYST_BUD)      unitValue = 1;      // fragment = 1 unit
        else if (toMat == Material.AMETHYST_SHARD)      unitValue = 64;     // shard = 64 units
        else /* AMETHYST_CLUSTER */                     unitValue = 4096;   // cluster = 4096 units

        ItemStack pretty = gemsManager.createCurrencyItem(toMat, toAmt, unitValue);
        p.getInventory().addItem(pretty);

        // 5) Send feedback and reopen GUI
        p.sendMessage(ChatColor.GREEN
            + "Converted " + fromAmt + " "
            + fromMat.name().toLowerCase().replace('_',' ')
            + " into " + toAmt + " "
            + toMat.name().toLowerCase().replace('_',' ') + "!");

        new BukkitRunnable() {
            @Override
            public void run() {
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

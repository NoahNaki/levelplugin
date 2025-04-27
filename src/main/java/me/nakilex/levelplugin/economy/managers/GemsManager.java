package me.nakilex.levelplugin.economy.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GemsManager {

    private static final Material FRAGMENT = Material.MEDIUM_AMETHYST_BUD;
    private static final Material SHARD    = Material.AMETHYST_SHARD;
    private static final Material CLUSTER  = Material.AMETHYST_CLUSTER;

    private static final int PER_SHARD   =   64;
    private static final int PER_CLUSTER = PER_SHARD * PER_SHARD; // 4096

    /** Total gem‐units in the player’s inventory. */
    public int getTotalUnits(Player player) {
        PlayerInventory inv = player.getInventory();
        int fragments = inv.all(FRAGMENT).values().stream().mapToInt(ItemStack::getAmount).sum();
        int shards    = inv.all(SHARD).   values().stream().mapToInt(ItemStack::getAmount).sum();
        int clusters  = inv.all(CLUSTER). values().stream().mapToInt(ItemStack::getAmount).sum();
        return fragments
            + shards   * PER_SHARD
            + clusters * PER_CLUSTER;
    }

    private ItemStack createCurrencyItem(Material mat, int qty, int unitValue) {
        ItemStack stack = new ItemStack(mat, qty);
        ItemMeta meta   = stack.getItemMeta();
        if (meta != null) {
            // 1) Name
            String baseName;
            if (mat == FRAGMENT) baseName = "Gem Fragment";
            else if (mat == SHARD) baseName = "Gem Shard";
            else /* mat==CLUSTER */  baseName = "Gem Cluster";

            meta.setDisplayName(ChatColor.LIGHT_PURPLE + baseName);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Currency");
            lore.add("");  // blank spacer

            // 3) Decorative count line:
            int totalUnits = qty * unitValue;
            String formatted = String.format("%,d ✦", totalUnits);

// styling chunks
            String dashFmt = ChatColor.DARK_PURPLE.toString()
                + ChatColor.BOLD
                + ChatColor.STRIKETHROUGH;
            String reset   = ChatColor.RESET.toString();
            String midFmt  = ChatColor.LIGHT_PURPLE.toString()
                + ChatColor.BOLD;

            String line = dashFmt + "---"           // strike the three hyphens
                + reset   + "{ "            // reset before brace
                + midFmt  + formatted       // number in light-purple bold
                + reset   + " }"            // reset after brace
                + dashFmt + "---";          // strike trailing hyphens

            lore.add(line);



            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Wipes all gem‐items from inventory and re‐distributes
     * them into the minimal number of clusters/shards/fragments.
     */
    public void setTotalUnits(Player player, int units) {
        var inv = player.getInventory();
        inv.remove(CLUSTER);
        inv.remove(SHARD);
        inv.remove(FRAGMENT);

        if (units <= 0) return;

        int clusters = units / PER_CLUSTER;
        int rem      = units % PER_CLUSTER;
        int shards   = rem / PER_SHARD;
        int frags    = rem % PER_SHARD;

        // give back with pretty meta
        if (clusters > 0) inv.addItem(createCurrencyItem(CLUSTER, clusters, PER_CLUSTER));
        if (shards   > 0) inv.addItem(createCurrencyItem(SHARD,    shards,    PER_SHARD));
        if (frags    > 0) inv.addItem(createCurrencyItem(FRAGMENT, frags, 1));
    }

    /** Add units (can be >4096) to the player. */
    public void addUnits(Player player, int units) {
        int current = getTotalUnits(player);
        setTotalUnits(player, current + units);
    }

    /** Deduct units; throws if insufficient. */
    public void deductUnits(Player player, int units) {
        int current = getTotalUnits(player);
        if (current < units) {
            throw new IllegalArgumentException("Not enough gems!");
        }
        setTotalUnits(player, current - units);
    }

    /** Get the “breakdown” into [clusters, shards, fragments]. */
    public int[] breakdown(Player player) {
        int total    = getTotalUnits(player);
        int clusters = total / PER_CLUSTER;
        int rem      = total % PER_CLUSTER;
        int shards   = rem / PER_SHARD;
        int frags    = rem % PER_SHARD;
        return new int[]{clusters, shards, frags};
    }
}

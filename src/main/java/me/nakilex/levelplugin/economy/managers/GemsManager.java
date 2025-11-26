package me.nakilex.levelplugin.economy.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.nakilex.levelplugin.economy.managers.TieredCurrencyManager;

public class GemsManager {

    private static final Material FRAGMENT = Material.MEDIUM_AMETHYST_BUD;
    private static final Material SHARD    = Material.AMETHYST_SHARD;
    private static final Material CLUSTER  = Material.AMETHYST_CLUSTER;

    private static final int PER_SHARD   =   64;
    private static final int PER_CLUSTER = PER_SHARD * PER_SHARD; // 4096

    private final TieredCurrencyManager delegate = TieredCurrencyManager.builder("Gem", "Gems", ChatColor.LIGHT_PURPLE)
            .tier("Gem Fragment", FRAGMENT, 1)
            .tier("Gem Shard", SHARD, PER_SHARD)
            .tier("Gem Cluster", CLUSTER, PER_CLUSTER)
            .build();

    /** Total gem‐units in the player’s inventory. */
    public int getTotalUnits(Player player) {
        return delegate.getTotalUnits(player);
    }

    public ItemStack createCurrencyItem(Material mat, int qty, int unitValue) {
        return delegate.createCurrencyItem(mat, qty, unitValue);
    }

    /**
     * Wipes all gem‐items from inventory and re‐distributes
     * them into the minimal number of clusters/shards/fragments.
     */
    public void setTotalUnits(Player player, int units) {
        delegate.setTotalUnits(player, units);
    }

    /** Add units (can be >4096) to the player. */
    public void addUnits(Player player, int units) {
        delegate.addUnits(player, units);
    }

    /** Deduct units; throws if insufficient. */
    public void deductUnits(Player player, int units) {
        delegate.deductUnits(player, units);
    }

    /** Get the “breakdown” into [clusters, shards, fragments]. */
    public int[] breakdown(Player player) {
        return delegate.breakdown(player);
    }
}

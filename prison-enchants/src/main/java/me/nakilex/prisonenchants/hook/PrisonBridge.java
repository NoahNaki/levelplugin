package me.nakilex.prisonenchants.hook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Sends generated block breaks through Prison's documented explosive event. */
public final class PrisonBridge {
    private final JavaPlugin plugin;
    private final EdPrisonBridge edPrison;
    private Constructor<?> explosiveEventConstructor;

    public PrisonBridge(JavaPlugin plugin, EdPrisonBridge edPrison) {
        this.plugin = plugin;
        this.edPrison = edPrison;
        connect();
    }

    private void connect() {
        try {
            Class<?> type = Class.forName("tech.mcprison.prison.spigot.api.ExplosiveBlockBreakEvent");
            explosiveEventConstructor = type.getConstructor(Block.class, Player.class, List.class, String.class);
            plugin.getLogger().info("Connected to Prison's ExplosiveBlockBreakEvent.");
        } catch (ReflectiveOperationException ex) {
            explosiveEventConstructor = null;
            plugin.getLogger().warning("Prison explosive event unavailable; using direct block fallback.");
        }
    }

    public int process(Player player, List<Block> rawBlocks, String enchantId) {
        List<Block> blocks = new ArrayList<>(new LinkedHashSet<>(rawBlocks));
        blocks.removeIf(block -> block == null || block.getType().isAir() || block.isLiquid());
        if (blocks.isEmpty()) return 0;

        boolean accepted = firePrisonEvent(player, blocks, enchantId);
        if (!accepted) return 0;

        if (plugin.getConfig().getBoolean("rewards.credit-edprison-pickaxe-blocks", true)) {
            String currency = plugin.getConfig().getString(
                    "rewards.edprison-pickaxe-block-currency", "pickaxeblocks");
            edPrison.addCurrency(player.getUniqueId(), currency, blocks.size());
        }
        return blocks.size();
    }

    private boolean firePrisonEvent(Player player, List<Block> blocks, String enchantId) {
        if (explosiveEventConstructor != null
                && plugin.getConfig().getBoolean("rewards.use-prison-explosive-event", true)) {
            try {
                Event event = (Event) explosiveEventConstructor.newInstance(
                        blocks.getFirst(), player, blocks, "PrisonEnchants:" + enchantId);
                Bukkit.getPluginManager().callEvent(event);
                return !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
            } catch (ReflectiveOperationException ex) {
                plugin.getLogger().warning("Prison rejected " + enchantId + " blocks: " + ex.getMessage());
                return false;
            }
        }

        // Development fallback. Production should always have Prison installed.
        Map<Material, Integer> drops = new HashMap<>();
        for (Block block : blocks) {
            drops.merge(block.getType(), 1, Integer::sum);
            block.setType(Material.AIR, false);
        }
        for (Map.Entry<Material, Integer> entry : drops.entrySet()) {
            int remaining = entry.getValue();
            while (remaining > 0) {
                int amount = Math.min(entry.getKey().getMaxStackSize(), remaining);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(entry.getKey(), amount));
                overflow.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
                remaining -= amount;
            }
        }
        return true;
    }
}

package me.nakilex.levelplugin.player.mining.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager;
import me.nakilex.levelplugin.items.tools.MiningToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles kingdom mine regeneration gameplay for per-player environment worlds.
 */
public final class KingdomMineRegenListener implements Listener {

    private static final long REGEN_TICKS = 20L * 10L;

    private final Main plugin;
    private final EnvironmentAreaInstanceManager areaManager;
    private final Map<Material, Material> oreFallback = new HashMap<>();
    private final Map<Material, Integer> miningXpByMaterial = new HashMap<>();
    private final java.util.Set<Material> allowedMineMaterials = new java.util.HashSet<>();

    public KingdomMineRegenListener(Main plugin, EnvironmentAreaInstanceManager areaManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
        registerOreFallbacks();
        registerXpValues();
        registerAllowedMineMaterials();
    }

    private void registerOreFallbacks() {
        for (Material material : Material.values()) {
            if (material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS) {
                if (material.name().startsWith("DEEPSLATE_")) oreFallback.put(material, Material.DEEPSLATE);
                else if (material.name().startsWith("NETHER_") || material == Material.ANCIENT_DEBRIS) oreFallback.put(material, Material.NETHERRACK);
                else oreFallback.put(material, Material.STONE);
            }
        }
    }

    private void registerXpValues() {
        miningXpByMaterial.put(Material.STONE, 2);
        miningXpByMaterial.put(Material.COBBLESTONE, 1);
        miningXpByMaterial.put(Material.COAL_ORE, 6);
        miningXpByMaterial.put(Material.DEEPSLATE_COAL_ORE, 7);
        miningXpByMaterial.put(Material.COPPER_ORE, 8);
        miningXpByMaterial.put(Material.DEEPSLATE_COPPER_ORE, 9);
        miningXpByMaterial.put(Material.IRON_ORE, 10);
        miningXpByMaterial.put(Material.DEEPSLATE_IRON_ORE, 11);
        miningXpByMaterial.put(Material.REDSTONE_ORE, 11);
        miningXpByMaterial.put(Material.DEEPSLATE_REDSTONE_ORE, 12);
        miningXpByMaterial.put(Material.LAPIS_ORE, 12);
        miningXpByMaterial.put(Material.DEEPSLATE_LAPIS_ORE, 13);
        miningXpByMaterial.put(Material.GOLD_ORE, 14);
        miningXpByMaterial.put(Material.DEEPSLATE_GOLD_ORE, 15);
        miningXpByMaterial.put(Material.DIAMOND_ORE, 18);
        miningXpByMaterial.put(Material.DEEPSLATE_DIAMOND_ORE, 20);
        miningXpByMaterial.put(Material.EMERALD_ORE, 20);
        miningXpByMaterial.put(Material.DEEPSLATE_EMERALD_ORE, 22);
        miningXpByMaterial.put(Material.NETHER_QUARTZ_ORE, 14);
        miningXpByMaterial.put(Material.NETHER_GOLD_ORE, 15);
        miningXpByMaterial.put(Material.ANCIENT_DEBRIS, 28);
    }


    private void registerAllowedMineMaterials() {
        allowedMineMaterials.add(Material.STONE);
        allowedMineMaterials.add(Material.COBBLESTONE);
        allowedMineMaterials.add(Material.DEEPSLATE);
        allowedMineMaterials.add(Material.NETHERRACK);
        allowedMineMaterials.add(Material.BEDROCK);
        allowedMineMaterials.addAll(oreFallback.keySet());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (event.isCancelled() || !areaManager.isMineBlock(player, block)) return;

        Material current = block.getType();
        if (!allowedMineMaterials.contains(current)) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }
        Material next = nextState(current, player);
        if (next == null) return;

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        rewardPlayer(player, block, current);

        BlockData original = block.getBlockData().clone();
        block.setType(next, false);

        if (next == Material.BEDROCK) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!block.getChunk().isLoaded()) block.getChunk().load();
                if (block.getType() == Material.BEDROCK && areaManager.isMineBlock(player, block)) {
                    block.setBlockData(original, false);
                }
            }, REGEN_TICKS);
        }
    }

    private void rewardPlayer(Player player, Block block, Material brokenMaterial) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        MiningToolEnchant enchant = ToolManager.getInstance().getMiningEnchant(tool);

        int xp = miningXpByMaterial.getOrDefault(brokenMaterial, 2);
        if (enchant == MiningToolEnchant.SCHOLAR && ThreadLocalRandom.current().nextDouble() <= 0.25D) {
            xp = (int) Math.round(xp * 1.5D);
        }
        MiningManager.getInstance().addXP(player, Math.max(1, xp));

        Material dropMaterial = resolveDropMaterial(brokenMaterial);
        if (dropMaterial == null || dropMaterial == Material.AIR) return;

        int dropAmount = 1;
        if (enchant == MiningToolEnchant.VEINSPARK && ThreadLocalRandom.current().nextDouble() <= 0.20D) {
            dropAmount++;
        }

        Item drop = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), new ItemStack(dropMaterial, dropAmount));
        drop.setPickupDelay(0);
    }

    private Material resolveDropMaterial(Material brokenMaterial) {
        return switch (brokenMaterial) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.RAW_GOLD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case NETHER_QUARTZ_ORE -> Material.QUARTZ;
            case ANCIENT_DEBRIS -> Material.ANCIENT_DEBRIS;
            case STONE -> Material.COBBLESTONE;
            case COBBLESTONE -> Material.COBBLESTONE;
            case DEEPSLATE -> Material.COBBLED_DEEPSLATE;
            default -> null;
        };
    }

    private Material nextState(Material material, Player player) {
        if (material == Material.STONE || material == Material.DEEPSLATE || material == Material.NETHERRACK) {
            return Material.COBBLESTONE;
        }
        if (material == Material.COBBLESTONE) {
            return Material.BEDROCK;
        }
        Material fallback = oreFallback.get(material);
        if (fallback != null) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            MiningToolEnchant enchant = ToolManager.getInstance().getMiningEnchant(tool);
            if (enchant == MiningToolEnchant.SHATTER && ThreadLocalRandom.current().nextDouble() <= 0.15D) {
                return Material.BEDROCK;
            }
            return fallback;
        }
        return null;
    }
}

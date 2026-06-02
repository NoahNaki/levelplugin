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

import java.util.EnumMap;
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
    private final Map<Material, MiningBlockDefinition> miningBlocks = new EnumMap<>(Material.class);
    private final java.util.Set<Material> allowedMineMaterials = new java.util.HashSet<>();

    public KingdomMineRegenListener(Main plugin, EnvironmentAreaInstanceManager areaManager) {
        this.plugin = plugin;
        this.areaManager = areaManager;
        registerOreFallbacks();
        registerMiningBlocks();
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

    private void registerMiningBlocks() {
        registerMiningBlock(Material.STONE, Material.COBBLESTONE, 2, null);
        registerMiningBlock(Material.COBBLESTONE, Material.COBBLESTONE, 1, null);
        registerMiningBlock(Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, 2, null);
        registerMiningBlock(Material.NETHERRACK, null, 2, null);
        registerMiningBlock(Material.COAL_ORE, Material.COAL, 6, "coal_ore");
        registerMiningBlock(Material.DEEPSLATE_COAL_ORE, Material.COAL, 7, "coal_ore");
        registerMiningBlock(Material.COPPER_ORE, Material.RAW_COPPER, 8, "copper_ore");
        registerMiningBlock(Material.DEEPSLATE_COPPER_ORE, Material.RAW_COPPER, 9, "copper_ore");
        registerMiningBlock(Material.IRON_ORE, Material.RAW_IRON, 10, "iron_ore");
        registerMiningBlock(Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON, 11, "iron_ore");
        registerMiningBlock(Material.REDSTONE_ORE, Material.REDSTONE, 11, "redstone_ore");
        registerMiningBlock(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE, 12, "redstone_ore");
        registerMiningBlock(Material.LAPIS_ORE, Material.LAPIS_LAZULI, 12, "lapis_ore");
        registerMiningBlock(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_LAZULI, 13, "lapis_ore");
        registerMiningBlock(Material.GOLD_ORE, Material.RAW_GOLD, 14, "gold_ore");
        registerMiningBlock(Material.DEEPSLATE_GOLD_ORE, Material.RAW_GOLD, 15, "gold_ore");
        registerMiningBlock(Material.DIAMOND_ORE, Material.DIAMOND, 18, "diamond_ore");
        registerMiningBlock(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND, 20, "diamond_ore");
        registerMiningBlock(Material.EMERALD_ORE, Material.EMERALD, 20, "emerald_ore");
        registerMiningBlock(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD, 22, "emerald_ore");
        registerMiningBlock(Material.NETHER_QUARTZ_ORE, Material.QUARTZ, 14, "quartz_ore");
        registerMiningBlock(Material.NETHER_GOLD_ORE, Material.RAW_GOLD, 15, "gold_ore");
        registerMiningBlock(Material.ANCIENT_DEBRIS, Material.ANCIENT_DEBRIS, 28, "netherite_ore");
    }

    private void registerMiningBlock(Material blockMaterial, Material dropMaterial, int xp, String questOreId) {
        miningBlocks.put(blockMaterial, new MiningBlockDefinition(dropMaterial, xp, questOreId));
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

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        ItemStack tool = player.getInventory().getItemInMainHand();
        MiningToolEnchant enchant = ToolManager.getInstance().getMiningEnchant(tool);
        processBlock(player, block, enchant);

        if (enchant == MiningToolEnchant.QUARRY) {
            for (Block extra : getQuarryNeighbors(block, player)) {
                processBlock(player, extra, enchant);
            }
        }
    }


    private void processBlock(Player player, Block block, MiningToolEnchant enchant) {
        if (block == null || !areaManager.isMineBlock(player, block)) return;
        Material current = block.getType();
        if (!allowedMineMaterials.contains(current)) return;
        Material next = nextState(current);
        if (next == null) return;

        BlockData original = block.getBlockData().clone();
        if (enchant == MiningToolEnchant.DEEPCORE) {
            rewardPlayer(player, block, current);
            rewardStageMaterial(player, block, next);
            block.setType(Material.BEDROCK, false);
            scheduleRegen(player, block, original);
            return;
        }

        rewardPlayer(player, block, current);
        block.setType(next, false);

        if (next == Material.BEDROCK) {
            scheduleRegen(player, block, original);
        }
    }

    private java.util.List<Block> getQuarryNeighbors(Block origin, Player player) {
        java.util.List<Block> out = new java.util.ArrayList<>(8);
        org.bukkit.block.BlockFace face = player.getTargetBlockFace(6);
        if (face == null) face = org.bukkit.block.BlockFace.UP;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                Block target;
                if (face == org.bukkit.block.BlockFace.UP || face == org.bukkit.block.BlockFace.DOWN) {
                    target = origin.getRelative(a, 0, b);
                } else if (face == org.bukkit.block.BlockFace.NORTH || face == org.bukkit.block.BlockFace.SOUTH) {
                    target = origin.getRelative(a, b, 0);
                } else {
                    target = origin.getRelative(0, b, a);
                }
                out.add(target);
            }
        }
        return out;
    }


    private void rewardStageMaterial(Player player, Block block, Material stageMaterial) {
        Material dropMaterial = resolveDropMaterial(stageMaterial);
        if (dropMaterial == null || dropMaterial == Material.AIR) return;
        Item drop = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), new ItemStack(dropMaterial, 1));
        drop.setPickupDelay(0);
    }

    private void scheduleRegen(Player player, Block block, BlockData original) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!block.getChunk().isLoaded()) block.getChunk().load();
            if (block.getType() == Material.BEDROCK && areaManager.isMineBlock(player, block)) {
                block.setBlockData(original, false);
            }
        }, REGEN_TICKS);
    }

    private void rewardPlayer(Player player, Block block, Material brokenMaterial) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        MiningToolEnchant enchant = ToolManager.getInstance().getMiningEnchant(tool);

        MiningBlockDefinition definition = miningBlocks.get(brokenMaterial);
        int xp = definition != null ? definition.xp() : 2;
        if (enchant == MiningToolEnchant.INSIGHT && ThreadLocalRandom.current().nextDouble() <= 0.30D) {
            xp = (int) Math.round(xp * 1.6D);
        }
        MiningManager.getInstance().addXP(player, Math.max(1, xp));
        if (definition != null && definition.questOreId() != null && plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleMineOre(player, definition.questOreId());
        }

        Material dropMaterial = resolveDropMaterial(brokenMaterial);
        if (dropMaterial == null || dropMaterial == Material.AIR) return;

        int dropAmount = 1;

        Item drop = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), new ItemStack(dropMaterial, dropAmount));
        drop.setPickupDelay(0);
    }

    private Material resolveDropMaterial(Material brokenMaterial) {
        MiningBlockDefinition definition = miningBlocks.get(brokenMaterial);
        return definition != null ? definition.dropMaterial() : null;
    }

    private Material nextState(Material material) {
        if (material == Material.STONE || material == Material.DEEPSLATE || material == Material.NETHERRACK) {
            return Material.COBBLESTONE;
        }
        if (material == Material.COBBLESTONE) {
            return Material.BEDROCK;
        }
        Material fallback = oreFallback.get(material);
        if (fallback != null) {
            return fallback;
        }
        return null;
    }

    private record MiningBlockDefinition(Material dropMaterial, int xp, String questOreId) {
    }

}

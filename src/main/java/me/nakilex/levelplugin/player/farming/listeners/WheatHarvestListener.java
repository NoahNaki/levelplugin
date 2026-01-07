package me.nakilex.levelplugin.player.farming.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.FarmingToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.farming.data.FarmingCrop;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.FullInventoryListener;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;

public class WheatHarvestListener implements Listener {

    private final FarmingManager farmingManager;
    private final Map<Location, MultiLineHologram> specialCrops = new HashMap<>();
    private static final double SPECIAL_CROP_CHANCE = 0.005;
    private static final double ABUNDANCE_CHANCE = 0.03;
    private static final int ABUNDANCE_RADIUS = 5;

    public WheatHarvestListener(FarmingManager farmingManager) {
        this.farmingManager = farmingManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (me.nakilex.levelplugin.utils.WorldExclusionUtil.isExcluded(event.getPlayer())) {
            return;
        }
        Block block = event.getBlock();
        FarmingCrop crop = FarmingCrop.fromBlock(block);
        if (crop == null) return;

        event.setCancelled(true);
        handleHarvest(event.getPlayer(), block, crop);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        FarmingCrop crop = FarmingCrop.fromBlock(block);
        if (block == null || crop == null) return;
        Player player = event.getPlayer();
        if (me.nakilex.levelplugin.utils.WorldExclusionUtil.isExcluded(player)) {
            return;
        }
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        event.setCancelled(true);
        handleHarvest(player, block, crop);
    }

    private void handleHarvest(Player player, Block block, FarmingCrop crop) {
        if (!crop.isMature(block)) {
            return;
        }
        int level = farmingManager.getLevel(player);
        if (level < crop.getLevelRequirement()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need Farming level " + crop.getLevelRequirement() + " to harvest that crop.");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        me.nakilex.levelplugin.items.tools.CustomTool tool = held != null ? ToolManager.getInstance().getTool(held) : null;
        boolean isFarmingTool = tool != null && tool.getDiscipline() == ToolDiscipline.FARMING;
        if (!isFarmingTool) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Hold a farming scythe to harvest crops.");
            return;
        }
        if (isFarmingTool && !ToolManager.getInstance().meetsLevelRequirement(player, tool)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need Farming level " + tool.getTier().getLevelRequirement() + " to use this scythe.");
            return;
        }
        FarmingToolEnchant enchant = isFarmingTool ? ToolManager.getInstance().getFarmingEnchant(held) : null;
        boolean reaping = enchant == FarmingToolEnchant.REAPING;
        boolean bountiful = enchant == FarmingToolEnchant.BOUNTIFUL;
        boolean abundance = enchant == FarmingToolEnchant.ABUNDANCE;
        boolean consistency = enchant == FarmingToolEnchant.CONSISTENCY;

        double baseYield = isFarmingTool ? tool.getTier().getHarvestYield() : 1.0;
        double yieldMultiplier = bountiful ? baseYield * 2.0 : baseYield;

        Set<Block> targets = new HashSet<>();
        if (abundance && ThreadLocalRandom.current().nextDouble() < ABUNDANCE_CHANCE) {
            collectCircleTargets(block, targets, ABUNDANCE_RADIUS);
        } else if (consistency) {
            int size = farmingManager.getConsistencySize(player);
            collectSquareTargets(block, targets, size);
        } else if (reaping) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block nearby = block.getRelative(dx, 0, dz);
                    FarmingCrop nearbyCrop = FarmingCrop.fromBlock(nearby);
                    if (nearbyCrop != null && nearbyCrop.isMature(nearby)) {
                        targets.add(nearby);
                    }
                }
            }
        } else {
            targets.add(block);
        }

        int harvested = 0;
        for (Block target : targets) {
            FarmingCrop targetCrop = FarmingCrop.fromBlock(target);
            if (targetCrop == null || !targetCrop.isMature(target)) {
                continue;
            }
            if (level < targetCrop.getLevelRequirement()) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "You need Farming level " + targetCrop.getLevelRequirement() + " to harvest "
                                + targetCrop.getItemMaterial().name().toLowerCase().replace('_', ' ') + ".");
                continue;
            }

            farmingManager.addXP(player, targetCrop.getXpReward());
            if (Main.getInstance().getQuestManager() != null) {
                Main.getInstance().getQuestManager().handleGatherCrops(player, targetCrop.getQuestId());
            }
            harvested++;

            int amount = Math.max(1, (int) Math.round(yieldMultiplier));
            ItemStack drop = new ItemStack(targetCrop.getItemMaterial(), amount);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
            if (!overflow.isEmpty()) {
                FullInventoryListener.sendFullInventoryTitle(player, Main.getInstance().getSettingsManager());
                overflow.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item));
            }

            clearSpecialCrop(target.getLocation());
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                targetCrop.replant(target);
                maybeSpawnSpecialCrop(target);
            }, 1L);
        }
        if (consistency && harvested > 0) {
            farmingManager.recordConsistencyHarvest(player, harvested);
        }
    }

    private void collectCircleTargets(Block center, Set<Block> targets, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }
                Block nearby = center.getRelative(dx, 0, dz);
                FarmingCrop nearbyCrop = FarmingCrop.fromBlock(nearby);
                if (nearbyCrop != null && nearbyCrop.isMature(nearby)) {
                    targets.add(nearby);
                }
            }
        }
    }

    private void collectSquareTargets(Block center, Set<Block> targets, int size) {
        int clamped = Math.max(1, size);
        int start = -((clamped - 1) / 2);
        int end = start + clamped - 1;
        for (int dx = start; dx <= end; dx++) {
            for (int dz = start; dz <= end; dz++) {
                Block nearby = center.getRelative(dx, 0, dz);
                FarmingCrop nearbyCrop = FarmingCrop.fromBlock(nearby);
                if (nearbyCrop != null && nearbyCrop.isMature(nearby)) {
                    targets.add(nearby);
                }
            }
        }
    }

    private void maybeSpawnSpecialCrop(Block block) {
        if (block == null) return;
        FarmingCrop crop = FarmingCrop.fromBlock(block);
        if (crop == null) return;
        if (ThreadLocalRandom.current().nextDouble() >= SPECIAL_CROP_CHANCE) {
            return;
        }
        Location loc = block.getLocation();
        clearSpecialCrop(loc);
        MultiLineHologram holo = new MultiLineHologram(loc.clone().add(0.5, 1.2, 0.5), "farming_special_crop");
        String stars = GuiUtil.glyphStars(2);
        holo.spawn(java.util.List.of("§6" + stars + " §eSpecial Crop §6" + stars));
        specialCrops.put(loc, holo);
    }

    private void clearSpecialCrop(Location loc) {
        if (loc == null) return;
        MultiLineHologram holo = specialCrops.remove(loc);
        if (holo != null) {
            holo.despawn();
        }
    }
}

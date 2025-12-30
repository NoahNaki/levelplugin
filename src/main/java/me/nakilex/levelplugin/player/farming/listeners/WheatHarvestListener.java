package me.nakilex.levelplugin.player.farming.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.FarmingToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.farming.data.FarmingCrop;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.FullInventoryListener;
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
import java.util.Map;
import java.util.Set;

public class WheatHarvestListener implements Listener {

    private final FarmingManager farmingManager;

    public WheatHarvestListener(FarmingManager farmingManager) {
        this.farmingManager = farmingManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
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
        FarmingToolEnchant enchant = isFarmingTool ? ToolManager.getInstance().getFarmingEnchant(held) : null;
        boolean reaping = enchant == FarmingToolEnchant.REAPING;
        boolean bountiful = enchant == FarmingToolEnchant.BOUNTIFUL;

        double baseYield = isFarmingTool ? tool.getTier().getHarvestYield() : 1.0;
        double yieldMultiplier = bountiful ? baseYield * 2.0 : baseYield;

        Set<Block> targets = new HashSet<>();
        if (reaping) {
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

            int amount = Math.max(1, (int) Math.round(yieldMultiplier));
            ItemStack drop = new ItemStack(targetCrop.getItemMaterial(), amount);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
            if (!overflow.isEmpty()) {
                FullInventoryListener.sendFullInventoryTitle(player, Main.getInstance().getSettingsManager());
                overflow.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item));
            }

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> targetCrop.replant(target), 1L);
        }
    }
}

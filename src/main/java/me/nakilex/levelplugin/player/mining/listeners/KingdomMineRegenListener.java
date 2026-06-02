package me.nakilex.levelplugin.player.mining.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.MiningToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig;
import me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig.MiningBlockReward;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
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

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles kingdom mine regeneration gameplay for per-player environment worlds.
 */
public final class KingdomMineRegenListener implements Listener {

    private static final long REGEN_TICKS = 20L * 10L;

    private final Main plugin;
    private final EnvironmentAreaInstanceManager areaManager;
    private final MiningRewardsConfig rewardsConfig;

    public KingdomMineRegenListener(Main plugin, EnvironmentAreaInstanceManager areaManager,
                                    MiningRewardsConfig rewardsConfig) {
        this.plugin = plugin;
        this.areaManager = areaManager;
        this.rewardsConfig = rewardsConfig;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (event.isCancelled() || !areaManager.isMineBlock(player, block)) return;

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        ItemStack toolStack = player.getInventory().getItemInMainHand();
        CustomTool tool = ToolManager.getInstance().getTool(toolStack);
        if (!isValidMiningTool(player, tool)) return;

        MiningToolEnchant enchant = ToolManager.getInstance().getMiningEnchant(toolStack);
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
        MiningBlockReward reward = rewardsConfig.getReward(current);
        if (reward == null || !meetsLevelRequirement(player, reward)) return;
        Material next = reward.replacementMaterial();
        if (next == null) return;

        BlockData original = block.getBlockData().clone();
        if (enchant == MiningToolEnchant.DEEPCORE) {
            rewardPlayer(player, block, reward);
            rewardStageMaterial(player, block, next);
            block.setType(Material.BEDROCK, false);
            scheduleRegen(player, block, original);
            return;
        }

        rewardPlayer(player, block, reward);
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

    private void rewardPlayer(Player player, Block block, MiningBlockReward reward) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        MiningToolEnchant enchant = ToolManager.getInstance().getMiningEnchant(tool);

        int xp = reward.xp();
        if (enchant == MiningToolEnchant.INSIGHT && ThreadLocalRandom.current().nextDouble() <= 0.30D) {
            xp = (int) Math.round(xp * 1.6D);
        }
        MiningManager.getInstance().addXP(player, Math.max(1, xp));
        if (reward.questOreId() != null && plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleMineOre(player, reward.questOreId());
        }

        Material dropMaterial = reward.dropMaterial();
        if (dropMaterial == null || dropMaterial == Material.AIR) return;

        int dropAmount = reward.dropMin();
        if (reward.dropMax() > dropAmount) {
            dropAmount += ThreadLocalRandom.current().nextInt(reward.dropMax() - dropAmount + 1);
        }
        if (dropAmount <= 0) return;

        Item drop = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), new ItemStack(dropMaterial, dropAmount));
        drop.setPickupDelay(0);
    }

    private Material resolveDropMaterial(Material brokenMaterial) {
        MiningBlockReward reward = rewardsConfig.getReward(brokenMaterial);
        return reward != null ? reward.dropMaterial() : null;
    }

    private boolean isValidMiningTool(Player player, CustomTool tool) {
        if (tool == null || tool.getDiscipline() != ToolDiscipline.MINING) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Hold a mining pickaxe to mine blocks in your kingdom mine.");
            return false;
        }
        if (!ToolManager.getInstance().meetsLevelRequirement(player, tool)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You need Mining level " + tool.getTier().getLevelRequirement() + " to use this pickaxe.");
            return false;
        }
        return true;
    }

    private boolean meetsLevelRequirement(Player player, MiningBlockReward reward) {
        if (MiningManager.getInstance().getLevel(player) >= reward.levelRequirement()) return true;
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "You need Mining level " + reward.levelRequirement() + " to mine that block.");
        return false;
    }

}

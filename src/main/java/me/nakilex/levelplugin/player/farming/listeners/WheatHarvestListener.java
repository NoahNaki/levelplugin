package me.nakilex.levelplugin.player.farming.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.utils.FullInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class WheatHarvestListener implements Listener {

    private final FarmingManager farmingManager;

    public WheatHarvestListener(FarmingManager farmingManager) {
        this.farmingManager = farmingManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.WHEAT) return;

        event.setCancelled(true);
        handleHarvest(event.getPlayer(), block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WHEAT) return;
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        event.setCancelled(true);
        handleHarvest(player, block);
    }

    private void handleHarvest(Player player, Block block) {
        Ageable ageable = (Ageable) block.getBlockData();
        int age = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        if (age < maxAge) {
            return;
        }

        farmingManager.addXP(player, 12);
        if (Main.getInstance().getQuestManager() != null) {
            Main.getInstance().getQuestManager().handleGatherCrops(player, "WHEAT");
        }

        double yieldMultiplier = 1.0;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null) {
            me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(held);
            if (tool != null && tool.getDiscipline() == ToolDiscipline.FARMING) {
                yieldMultiplier = tool.getTier().getHarvestYield();
            }
        }

        int wheatAmount = Math.max(1, (int) Math.round(yieldMultiplier));
        ItemStack wheatDrop = new ItemStack(Material.WHEAT, wheatAmount);
        Item dropped = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), wheatDrop);
        dropped.setOwner(player.getUniqueId());
        dropped.setPickupDelay(20);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline() || !dropped.isValid()) {
                if (dropped.isValid()) {
                    dropped.remove();
                }
                return;
            }

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(dropped.getItemStack());
            dropped.remove();
            if (!overflow.isEmpty()) {
                FullInventoryListener.sendFullInventoryTitle(player, Main.getInstance().getSettingsManager());
                overflow.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
        }, 20L);

        // Replant seedling
        block.setType(Material.WHEAT);
        Ageable replanted = (Ageable) block.getBlockData();
        replanted.setAge(0);
        block.setBlockData(replanted);
    }
}

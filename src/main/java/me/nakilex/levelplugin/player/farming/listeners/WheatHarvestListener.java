package me.nakilex.levelplugin.player.farming.listeners;

import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

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
        Player player = event.getPlayer();

        Ageable ageable = (Ageable) block.getBlockData();
        int age = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        float maturity = (float) age / maxAge;

        int baseXp = age >= maxAge ? 12 : 1;
        int xpAward = Math.max(1, baseXp);
        farmingManager.addXP(player, xpAward);

        double baseYield = age >= maxAge ? 1.0 : 0.15;
        double maturityYield = Math.max(0.2, baseYield + maturity * 0.5);

        double yieldMultiplier = 1.0;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null) {
            me.nakilex.levelplugin.items.tools.CustomTool tool = ToolManager.getInstance().getTool(held.getType());
            if (tool != null && tool.getDiscipline() == ToolDiscipline.FARMING) {
                yieldMultiplier = tool.getTier().getHarvestYield();
            }
        }

        int wheatAmount = Math.max(1, (int) Math.round(maturityYield * yieldMultiplier));
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), new ItemStack(Material.WHEAT, wheatAmount));

        // Replant seedling
        block.setType(Material.WHEAT);
        Ageable replanted = (Ageable) block.getBlockData();
        replanted.setAge(0);
        block.setBlockData(replanted);

        if (age < maxAge) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "§7The crop was not fully grown. You earned minimal farming XP.");
        }
    }
}

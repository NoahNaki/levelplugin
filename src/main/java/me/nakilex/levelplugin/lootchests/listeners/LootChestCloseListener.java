package me.nakilex.levelplugin.lootchests.listeners;

import com.nexo.api.NexoFurniture;
import com.nexo.api.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Random;

public class LootChestCloseListener implements Listener {

    private final LootChestManager lootChestManager;
    private final EconomyManager economyManager;
    private final Random random = new Random();

    // e.g. 40% chance to find coins
    private static final double COIN_CHANCE = 0.4;

    public LootChestCloseListener(LootChestManager lootChestManager,
                                  EconomyManager economyManager) {
        this.lootChestManager = lootChestManager;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 1) Look up which chest the player was viewing
        Player player = (Player) event.getPlayer();
        Integer chestId = lootChestManager.unmarkPlayerViewingChest(player.getUniqueId());
        if (chestId == null) {
            return; // Player wasn’t viewing a loot‐chest GUI
        }

        // 2) Verify the crate still exists at its stored location
        Location loc = lootChestManager.getLocationForChestId(chestId);
        if (loc == null) {
            return;
        }
        FurnitureMechanic mechAtLoc = NexoFurniture.getFurnitureMechanic(loc.getBlock());
        if (mechAtLoc == null || !mechAtLoc.getItemID().equals("crate_lvl1")) {
            return; // No crate there anymore
        }

        // 3) Drop random coins (using the EconomyManager instance)
        if (Math.random() < COIN_CHANCE) {
            int tier = lootChestManager.getTierForChest(chestId);
            int min, max;
            if (tier <= 2) {
                min = 10; max = 20;
            } else if (tier <= 4) {
                min = 25; max = 40;
            } else {
                min = 50; max = 75;
            }
            int coinAmount = random.nextInt(max - min + 1) + min;
            economyManager.addCoins(player, coinAmount);
            player.sendMessage(ChatColor.GRAY +
                "You found " + ChatColor.YELLOW + coinAmount + " ⛃" +
                ChatColor.GRAY + " coins!");
        }

        // 4) Remove the crate and start its cooldown
        lootChestManager.getPlugin().getLogger().info(
            "[LootChestCloseListener] Chest " + chestId +
                " was closed. Removing crate & starting cooldown."
        );
        lootChestManager.removeChest(chestId);
        lootChestManager.getCooldownManager().startChestCooldown(chestId);
    }
}

package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

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
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest)) return;

        Chest chest = (Chest) holder;
        Block block = chest.getBlock();
        if (block.getType() != Material.CHEST) return;

        Integer chestId = lootChestManager.getChestIdAtLocation(block.getLocation());
        if (chestId != null) {
            Player player = (Player) event.getPlayer();

            // --- NEW: roll for coins ---
            if (Math.random() < COIN_CHANCE) {
                int tier = lootChestManager.getTierForChest(chestId);
                int min, max;
                switch (tier) {
                    case 1: min = 5;   max = 10;  break;
                    case 2: min = 20;  max = 40;  break;
                    case 3: min = 50;  max = 135;  break;
                    case 4: min = 135;  max = 250;  break;
                    case 5: min = 350;  max = 575;  break;
                    case 6: min = 575;  max = 1100; break;
                    case 7: min = 1575;  max = 2100; break;
                    case 8: min = 2575;  max = 3100; break;
                    case 9: min = 3575;  max = 4100; break;
                    case 10: min = 4575;  max = 5100; break;
                    default:
                        min = 10; max = 20;
                }
                int coins = random.nextInt(max - min + 1) + min;
                economyManager.addCoins(player, coins);
                player.sendMessage("§6You found §e" + coins + " ⛃ coins §6in the chest!");
            }
            // --- end coin logic ---

            lootChestManager.getPlugin().getLogger().info(
                "[LootChestCloseListener] Chest " + chestId +
                    " was closed. Removing chest & starting cooldown."
            );

            lootChestManager.removeChest(chestId);
            lootChestManager.getCooldownManager().startChestCooldown(chestId);

        } else {
            lootChestManager.getPlugin().getLogger().info(
                "[LootChestCloseListener] InventoryCloseEvent for a chest block, " +
                    "but it's not in spawnedChests map."
            );
        }
    }
}

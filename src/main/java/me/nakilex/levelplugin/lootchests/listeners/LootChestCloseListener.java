package me.nakilex.levelplugin.lootchests.listeners;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
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
        // 1) Only proceed if the inventory title is "Loot Chest #<id>"
        String title = event.getView().getTitle();
        if (!title.startsWith("Loot Chest #")) {
            return;
        }

        // 2) Parse out chestId
        int chestId;
        try {
            chestId = Integer.parseInt(title.split("#")[1]);
        } catch (NumberFormatException ex) {
            return;
        }

        // 3) Verify crate still exists at stored location
        Location loc = lootChestManager.getLocationForChestId(chestId);
        if (loc == null) {
            return;
        }
        FurnitureMechanic mechAtLoc = OraxenFurniture.getFurnitureMechanic(loc.getBlock());
        if (mechAtLoc == null || !mechAtLoc.getItemID().equals("crate_lvl1")) {
            return; // no crate there
        }

        // 4) Drop random coins (using the instance of EconomyManager we were given)
        Player player = (Player) event.getPlayer();
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
            player.sendMessage(ChatColor.GOLD + "You found " + ChatColor.YELLOW + coinAmount + " ⛃" + ChatColor.GOLD + " coins!");
        }

        // 5) Remove the crate & start cooldown
        lootChestManager.getPlugin().getLogger().info(
            "[LootChestCloseListener] Chest " + chestId +
                " was closed. Removing crate & starting cooldown."
        );
        lootChestManager.removeChest(chestId);
        lootChestManager.getCooldownManager().startChestCooldown(chestId);
    }
}

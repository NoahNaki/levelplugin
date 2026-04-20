package me.nakilex.levelplugin.lootchests.listeners;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class LootChestCloseListener implements Listener {

    private final LootChestManager lootChestManager;
    private final EconomyManager economyManager;
    private final DungeonManager dungeonManager;

    public LootChestCloseListener(LootChestManager lootChestManager,
                                  EconomyManager economyManager,
                                  DungeonManager dungeonManager) {
        this.lootChestManager = lootChestManager;
        this.economyManager = economyManager;
        this.dungeonManager = dungeonManager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 1) Look up which chest the player was viewing
        Player player = (Player) event.getPlayer();
        LootChestManager.LootSession session = lootChestManager.consumeSession(player.getUniqueId());
        if (session == null) {
            return; // Player wasn’t viewing a loot‐chest GUI
        }
        int chestId = session.chestId();

        // 2) Verify the crate still exists at its stored location
        Location loc = lootChestManager.getLocationForChestId(chestId);
        if (loc == null) {
            return;
        }
        FurnitureMechanic mechAtLoc = NexoFurniture.furnitureMechanic(loc.getBlock());
        if (!lootChestManager.isLootChestMechanic(mechAtLoc)) {
            return; // No crate there anymore
        }

        // 3) Pay out coins scaled to the player's gear score for this session
        int coinAmount = Math.max(0, session.coinReward());
        if (coinAmount > 0) {
            economyManager.addCoins(player, coinAmount);
            me.nakilex.levelplugin.utils.CurrencyMessageUtil.sendReceive(player,
                    me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, coinAmount);
            if (session.bonusCoinReward() > 0) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                        ChatColor.GOLD + "Loot streak x" + session.streak()
                                + ChatColor.GRAY + ": +" + ChatColor.GOLD + session.bonusCoinReward()
                                + ChatColor.GRAY + " bonus coins.");
            }
        }

        // 4) Remove the crate and start its cooldown
        lootChestManager.getPlugin().getLogger().info(
            "[LootChestCloseListener] Chest " + chestId +
                " was closed. Removing crate & starting cooldown."
        );
        lootChestManager.removeChest(chestId);

        if (!dungeonManager.isInstanceWorld(loc.getWorld())) {
            lootChestManager.getCooldownManager().startChestCooldown(chestId);
        }
    }
}

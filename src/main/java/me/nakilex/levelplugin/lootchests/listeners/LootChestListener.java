package me.nakilex.levelplugin.lootchests.listeners;

import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;

    public LootChestListener(LootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        if (event.getClickedBlock().getType() == Material.CHEST) {
            Location loc = event.getClickedBlock().getLocation();
            Integer chestId = lootChestManager.getChestIdAtLocation(loc);
            if (chestId != null) {
                // Optional: do something if you want
                // e.g. event.getPlayer().sendMessage("You are opening a loot chest...");
                // We do NOT remove or cancel here so the chest UI can open
            }
        }
    }
}

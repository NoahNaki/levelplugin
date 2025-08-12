package me.nakilex.levelplugin.environment.stage;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import me.nakilex.levelplugin.environment.stage.StageSelectionStore;

/**
 * Shared wand interaction handler that records position selections using the
 * {@link StageSelectionStore} for both town and building stage commands.
 */
public class StageSelectionListener implements Listener {
    private final ItemStack wand = StageSelectionStore.WAND;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack inHand = event.getItem();
        if (inHand == null || !inHand.isSimilar(wand)) return;
        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        if (event.getAction().name().contains("LEFT")) {
            StageSelectionStore.setPos1(player.getUniqueId(), loc);
            player.sendMessage(ChatColor.AQUA + "Pos1 set " + format(loc));
        } else if (event.getAction().name().contains("RIGHT")) {
            StageSelectionStore.setPos2(player.getUniqueId(), loc);
            player.sendMessage(ChatColor.AQUA + "Pos2 set " + format(loc));
        }
    }

    private static String format(Location loc) {
        return loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
    }
}

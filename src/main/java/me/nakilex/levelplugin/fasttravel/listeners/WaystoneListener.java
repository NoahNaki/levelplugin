package me.nakilex.levelplugin.fasttravel.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fakeblock.ModelGate;
import me.nakilex.levelplugin.fakeblock.ModelGateManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaystoneListener implements Listener {
    private final FastTravelGUI gui;
    private final FastTravelManager manager;
    private final ModelGateManager gateManager;

    public WaystoneListener(FastTravelGUI gui, FastTravelManager manager, ModelGateManager gateManager) {
        this.gui = gui;
        this.manager = manager;
        this.gateManager = gateManager;
    }

    @EventHandler
    public void onInteract(NexoFurnitureInteractEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBaseEntity().getLocation().getBlock().getLocation();
        ModelGate gate = gateManager.getGateAt(blockLoc);
        if (gate == null) {
            FurnitureMechanic mech = event.getMechanic();
            if (!"base_beacon_blue".equals(mech.getItemID()) && !"base_beacon_blue_inventory".equals(mech.getItemID())) {
                return;
            }
            // fallback for legacy gates without manager
        } else {
            event.setCancelled(true);
            if (gate.isClosed(player.getUniqueId())) {
                gate.setClosed(player.getUniqueId(), false);
                gate.apply(player, gateManager.getPlugin());
                // unlocking via waystone should not count as a discovery
                manager.unlock(player, gate.getId(), false);
                sendUnlockMessage(player, gate.getLocation(), gate.getId());
            }
            gui.open(player, gate);
            return;
        }
        event.setCancelled(true);
        gui.open(player);
    }

    /**
     * Display a styled unlock message and play effects when a player activates a waystone.
     */
    private void sendUnlockMessage(Player player, Location loc, String id) {
        ChatFormatter.constructDivider(player, "", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lWaystone Unlocked!");
        ChatFormatter.sendCenteredMessage(player, "§e" + id);
        ChatFormatter.constructDivider(player, " ", 45);
        player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 1f);
        player.spawnParticle(Particle.FIREWORK, loc.add(0.5, 1, 0.5), 30, 0.3, 0.5, 0.3, 0.02);
    }
}

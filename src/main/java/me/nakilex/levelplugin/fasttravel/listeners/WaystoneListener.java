package me.nakilex.levelplugin.fasttravel.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fakeblock.ModelGate;
import me.nakilex.levelplugin.fakeblock.ModelGateManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.ChatColor;
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
        ModelGate gate = gateManager.getGateByEntity(event.getBaseEntity());
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
                manager.unlock(player, gate.getId());
                player.sendMessage(ChatColor.GOLD + "Waystone unlocked!");
            }
            gui.open(player);
            return;
        }
        event.setCancelled(true);
        gui.open(player);
    }
}

package me.nakilex.levelplugin.fasttravel.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaystoneListener implements Listener {
    private final FastTravelGUI gui;

    public WaystoneListener(FastTravelGUI gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onInteract(NexoFurnitureInteractEvent event) {
        FurnitureMechanic mech = event.getMechanic();
        //event.getPlayer().sendMessage("Debug clicked id: " + mech.getItemID());
        if (!"base_beacon_blue".equals(mech.getItemID()) &&
            !"base_beacon_blue_inventory".equals(mech.getItemID())) return;
        event.setCancelled(true);
        gui.open(event.getPlayer());
    }
}

package me.nakilex.levelplugin.fasttravel.listeners;

import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaystoneListener implements Listener {
    private final FastTravelGUI gui;

    public WaystoneListener(FastTravelGUI gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onInteract(OraxenFurnitureInteractEvent event) {
        FurnitureMechanic mech = event.getMechanic();
        if (!"base_beacon_blue".equals(mech.getItemID())) return;
        event.setCancelled(true);
        gui.open(event.getPlayer());
    }
}

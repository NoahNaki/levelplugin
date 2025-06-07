package me.nakilex.levelplugin.fasttravel.listeners;

import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class WaystoneListener implements Listener {
    private final FastTravelGUI gui;
    private final FastTravelManager manager;

    public WaystoneListener(FastTravelGUI gui, FastTravelManager manager) {
        this.gui = gui;
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(OraxenFurnitureInteractEvent event) {
        FurnitureMechanic mech = event.getMechanic();
        if (!"base_beacon_blue".equals(mech.getItemID())) return;
        event.setCancelled(true);
        gui.open(event.getPlayer());
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (manager.isWaystoneEntity(event.getRightClicked())) {
            event.setCancelled(true);
            gui.open(event.getPlayer());
        }
    }
}

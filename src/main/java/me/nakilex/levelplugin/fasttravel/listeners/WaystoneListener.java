package me.nakilex.levelplugin.fasttravel.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaystoneListener implements Listener {
    private final FastTravelGUI gui;
    private final FastTravelManager manager;

    public WaystoneListener(FastTravelGUI gui, FastTravelManager manager) {
        this.gui = gui;
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(NexoFurnitureInteractEvent event) {
        FurnitureMechanic mech = event.getMechanic();
        String id = mech.getItemID();
        if(!id.startsWith("base_beacon")) return;

        event.setCancelled(true);

        if("base_beacon_inert".equals(id)) {
            Location loc = event.getBaseEntity().getLocation();
            var pt = manager.getNearestPoint(loc, 5.0);
            if(pt != null && !manager.isUnlocked(event.getPlayer(), pt.getName())) {
                manager.unlock(event.getPlayer(), pt.getName());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Waystone unlocked!");
            }
        }

        gui.open(event.getPlayer());
    }
}

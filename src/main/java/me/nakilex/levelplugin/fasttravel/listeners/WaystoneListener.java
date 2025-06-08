package me.nakilex.levelplugin.fasttravel.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import me.nakilex.levelplugin.fasttravel.data.WaystoneType;
import me.nakilex.levelplugin.fasttravel.display.ClientWaystoneManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaystoneListener implements Listener {
    private final FastTravelGUI gui;
    private final FastTravelManager manager;
    private final ClientWaystoneManager display;

    public WaystoneListener(FastTravelGUI gui, FastTravelManager manager, ClientWaystoneManager display) {
        this.gui = gui;
        this.manager = manager;
        this.display = display;
    }

    @EventHandler
    public void onInteract(NexoFurnitureInteractEvent event) {
        FurnitureMechanic mech = event.getMechanic();
        String id = mech.getItemID();
        if(!id.startsWith("base_beacon")) return;

        event.setCancelled(true);

        if("base_beacon_inert".equals(id)) {
            Location loc = event.getBaseEntity().getLocation();
            var ws = manager.getNearestWaystone(loc, 5.0);
            if(ws != null && !manager.isUnlocked(event.getPlayer(), ws.getName())) {
                manager.getPlugin().getLogger().info("[WaystoneListener] Unlocking " + ws.getName() + " for " + event.getPlayer().getName());
                manager.unlock(event.getPlayer(), ws.getName());
                display.show(event.getPlayer(), ws);
                String color = ws.getType() == WaystoneType.TOWN ? ChatColor.BLUE.toString() : ChatColor.RED.toString();
                event.getPlayer().sendMessage(color + "Waystone unlocked!");
            }
        }

        gui.open(event.getPlayer());
    }
}

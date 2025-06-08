package me.nakilex.levelplugin.fasttravel.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import me.nakilex.levelplugin.fasttravel.data.WaystoneType;
import com.nexomc.nexo.api.NexoFurniture;
import org.bukkit.block.BlockFace;
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
            var ws = manager.getNearestWaystone(loc, 5.0);
            if(ws != null && !manager.isUnlocked(event.getPlayer(), ws.getName())) {
                manager.getPlugin().getLogger().info("[WaystoneListener] Unlocking " + ws.getName() + " for " + event.getPlayer().getName());
                manager.unlock(event.getPlayer(), ws.getName());
                String color = ws.getType() == WaystoneType.TOWN ? ChatColor.BLUE.toString() : ChatColor.RED.toString();
                boolean removed = NexoFurniture.remove(loc, null);
                manager.getPlugin().getLogger().info("[WaystoneListener] remove inert at " + loc + " -> " + removed);
                String newId = ws.getType() == WaystoneType.TOWN ? "base_beacon_blue" : "base_beacon_red";
                FurnitureMechanic newMech = NexoFurniture.furnitureMechanic(newId);
                if(newMech == null){
                    manager.getPlugin().getLogger().severe("[WaystoneListener] Furniture ID '"+newId+"' not registered");
                } else {
                    NexoFurniture.place(newId, loc, 0f, BlockFace.NORTH);
                    manager.getPlugin().getLogger().info("[WaystoneListener] placed " + newId + " at " + loc);
                    event.getPlayer().sendMessage(color + "Waystone unlocked!");
                }
            }
        }

        gui.open(event.getPlayer());
    }
}

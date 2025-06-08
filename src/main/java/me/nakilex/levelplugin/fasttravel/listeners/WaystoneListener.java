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
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import org.bukkit.persistence.PersistentDataType;

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

        if("base_beacon_blue".equals(id) || "base_beacon_red".equals(id)) {
            gui.open(event.getPlayer());
        }
    }

    @EventHandler
    public void onDisplayInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.ItemDisplay disp)) return;
        String name = disp.getPersistentDataContainer().get(ClientWaystoneManager.KEY, PersistentDataType.STRING);
        if (name == null) return;
        event.setCancelled(true);
        var ws = manager.getWaystone(name);
        if(ws != null && !manager.isUnlocked(event.getPlayer(), name)) {
            manager.unlock(event.getPlayer(), name);
            display.unlock(event.getPlayer(), ws);
            String color = ws.getType() == WaystoneType.TOWN ? ChatColor.BLUE.toString() : ChatColor.RED.toString();
            event.getPlayer().sendMessage(color + "Waystone unlocked!");
        }
        gui.open(event.getPlayer());
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;
        var player = event.getPlayer();
        var eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Waystone closest = null;
        double best = 2.0;
        for (var ws : manager.getWaystones()) {
            if (!ws.getLocation().getWorld().equals(eye.getWorld())) continue;
            Location center = ws.getLocation().clone().add(0.5, 1, 0.5);
            double dist = center.distance(eye);
            if (dist > best) continue;
            Vector to = center.toVector().subtract(eye.toVector()).normalize();
            if (to.dot(dir) < 0.95) continue;
            best = dist;
            closest = ws;
        }
        if (closest == null) return;
        event.setCancelled(true);
        if (!manager.isUnlocked(player, closest.getName())) {
            manager.unlock(player, closest.getName());
            display.unlock(player, closest);
            String color = closest.getType() == WaystoneType.TOWN ? ChatColor.BLUE.toString() : ChatColor.RED.toString();
            player.sendMessage(color + "Waystone unlocked!");
        }
        gui.open(player);
    }
}

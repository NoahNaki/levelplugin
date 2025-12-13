package me.nakilex.levelplugin.nexo;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple registry that maps Nexo furniture IDs to GUI openers.
 */
public class FurnitureGuiMapper implements Listener {

    private final Map<String, Consumer<Player>> guiOpeners = new HashMap<>();

    public void register(String furnitureId, Consumer<Player> opener) {
        if (furnitureId == null || opener == null) {
            return;
        }
        guiOpeners.put(furnitureId.toLowerCase(), opener);
    }

    @EventHandler
    public void onFurnitureInteract(NexoFurnitureInteractEvent event) {
        FurnitureMechanic mechanic = event.getMechanic();
        Consumer<Player> opener = guiOpeners.get(mechanic.getItemID().toLowerCase());
        if (opener == null) {
            return;
        }
        event.setCancelled(true);
        opener.accept(event.getPlayer());
    }
}

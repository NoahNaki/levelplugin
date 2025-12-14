package me.nakilex.levelplugin.nexo;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple registry that maps Nexo furniture IDs to GUI openers.
 */
public class FurnitureGuiMapper implements Listener {

    private final Map<String, Consumer<Player>> guiOpeners = new HashMap<>();
    private final Map<String, Consumer<Player>> proximityOpeners = new HashMap<>();
    private final Map<java.util.UUID, Long> proximityCooldowns = new HashMap<>();
    private static final long PROXIMITY_COOLDOWN_MS = 1500L;

    public void register(String furnitureId, Consumer<Player> opener) {
        if (furnitureId == null || opener == null) {
            return;
        }
        guiOpeners.put(furnitureId.toLowerCase(), opener);
    }

    public void registerProximity(String furnitureId, Consumer<Player> opener) {
        if (furnitureId == null || opener == null) {
            return;
        }
        proximityOpeners.put(furnitureId.toLowerCase(), opener);
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (proximityOpeners.isEmpty()) {
            return;
        }
        Location to = event.getTo();
        if (to == null || sameBlock(event.getFrom(), to)) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long until = proximityCooldowns.get(player.getUniqueId());
        if (until != null && until > now) {
            return;
        }
        Consumer<Player> opener = findNearbyOpener(to);
        if (opener == null) {
            return;
        }
        proximityCooldowns.put(player.getUniqueId(), now + PROXIMITY_COOLDOWN_MS);
        opener.accept(player);
    }

    private Consumer<Player> findNearbyOpener(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(world.getBlockAt(baseX + dx, baseY + dy, baseZ + dz));
                    if (mechanic == null) {
                        continue;
                    }
                    Consumer<Player> opener = proximityOpeners.get(mechanic.getItemID().toLowerCase());
                    if (opener != null) {
                        return opener;
                    }
                }
            }
        }
        return null;
    }

    private boolean sameBlock(Location from, Location to) {
        if (from == null || to == null) {
            return true;
        }
        Vector f = from.toVector();
        Vector t = to.toVector();
        return f.getBlockX() == t.getBlockX()
                && f.getBlockY() == t.getBlockY()
                && f.getBlockZ() == t.getBlockZ();
    }
}

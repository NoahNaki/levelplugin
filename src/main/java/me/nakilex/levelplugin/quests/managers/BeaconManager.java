package me.nakilex.levelplugin.quests.managers;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client-side beacon beams shown only to specific players.
 * Uses block change packets to fake a beacon and colored glass above it.
 */
public class BeaconManager implements Listener {

    private static class BeamData {
        final Location base;
        final BlockData baseOrig;
        final BlockData glassOrig;
        BeamData(Location base, BlockData baseOrig, BlockData glassOrig) {
            this.base = base;
            this.baseOrig = baseOrig;
            this.glassOrig = glassOrig;
        }
    }

    private final Map<UUID, BeamData> beams = new ConcurrentHashMap<>();

    /**
     * Show a colored beacon beam to a single player without modifying the world.
     *
     * @param player   the viewer
     * @param location the beacon base location
     * @param color    the beam color
     */
    public synchronized void showBeam(Player player, Location location, DyeColor color) {
        if (player == null || location == null) return;
        removeBeam(player);
        Location base = location.getBlock().getLocation();
        Location above = base.clone().add(0, 1, 0);

        beams.put(player.getUniqueId(), new BeamData(
                base,
                base.getBlock().getBlockData(),
                above.getBlock().getBlockData()
        ));

        player.sendBlockChange(base, Material.BEACON.createBlockData());
        Material glass = Material.valueOf(color.name() + "_STAINED_GLASS");
        player.sendBlockChange(above, glass.createBlockData());
    }

    /**
     * Remove any active beam for the player, restoring original blocks.
     */
    public synchronized void removeBeam(Player player) {
        BeamData data = beams.remove(player.getUniqueId());
        if (data != null) {
            player.sendBlockChange(data.base, data.baseOrig);
            player.sendBlockChange(data.base.clone().add(0, 1, 0), data.glassOrig);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBeam(event.getPlayer());
    }
}

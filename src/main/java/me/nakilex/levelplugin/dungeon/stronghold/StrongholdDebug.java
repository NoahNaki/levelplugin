package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.stronghold.StrongholdPlacement.PlacedRoom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Collection;
import java.util.logging.Logger;

/** Debug controls and structured placement logging hooks for stronghold generation. */
public final class StrongholdDebug {
    private boolean enabled;
    private double overlapTolerance = 0.0D;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setOverlapTolerance(double overlapTolerance) {
        this.overlapTolerance = Math.max(0.0D, overlapTolerance);
    }

    public double overlapTolerance() {
        return overlapTolerance;
    }

    public void logPlacement(Logger logger, String templateId, String rotation, String connectorPairing, String outcome) {
        if (!enabled || logger == null) {
            return;
        }
        logger.info("[StrongholdDebug] template=" + templateId
                + " rotation=" + rotation
                + " connectors=" + connectorPairing
                + " outcome=" + outcome);
    }

    public void render(Logger logger, Collection<PlacedRoom> rooms) {
        if (!enabled || logger == null) {
            return;
        }
        for (PlacedRoom room : rooms) {
            logger.info("[StrongholdDebug] bbox=" + room.worldBounds()
                    + " connectors=" + room.template().connectors().size()
                    + " template=" + room.template().id());
        }
    }

    public void renderPreview(Player player, Collection<PlacedRoom> rooms, int durationTicks) {
        if (!enabled || player == null || rooms == null || rooms.isEmpty()) {
            return;
        }
        Location origin = player.getLocation().clone();
        int ticks = Math.max(20, durationTicks);
        new BukkitRunnable() {
            private int lived = 0;

            @Override
            public void run() {
                if (!player.isOnline() || lived > ticks) {
                    cancel();
                    return;
                }
                for (PlacedRoom room : rooms) {
                    Location center = origin.clone().add(
                            room.transform().position().x() + 0.5,
                            room.transform().position().y() + 1.0,
                            room.transform().position().z() + 0.5
                    );
                    player.spawnParticle(Particle.END_ROD, center, 8, 0.4, 0.4, 0.4, 0.01);
                    for (var connector : room.template().connectors()) {
                        var rotated = connector.rotated(room.transform().rotation());
                        Location cLoc = origin.clone().add(
                                room.transform().position().x() + rotated.localPosition().x() + 0.5,
                                room.transform().position().y() + rotated.localPosition().y() + 1.0,
                                room.transform().position().z() + rotated.localPosition().z() + 0.5
                        );
                        player.spawnParticle(Particle.HAPPY_VILLAGER, cLoc, 5, 0.15, 0.15, 0.15, 0.0);
                    }
                }
                lived += 10;
            }
        }.runTaskTimer(JavaPlugin.getProvidingPlugin(StrongholdDebug.class), 0L, 10L);
    }
}

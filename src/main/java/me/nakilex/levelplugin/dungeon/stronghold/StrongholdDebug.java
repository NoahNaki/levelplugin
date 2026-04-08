package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.stronghold.StrongholdPlacement.PlacedRoom;
import org.bukkit.Color;
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
                    drawCuboidOutline(player, origin, room, Color.LIME);
                    for (var connector : room.template().connectors()) {
                        var rotated = connector.rotated(room.transform().rotation());
                        Location cLoc = origin.clone().add(
                                room.transform().position().x() + rotated.localPosition().x() + 0.5,
                                room.transform().position().y() + rotated.localPosition().y() + 1.0,
                                room.transform().position().z() + rotated.localPosition().z() + 0.5
                        );
                        player.spawnParticle(Particle.END_ROD, cLoc, 4, 0.10, 0.10, 0.10, 0.0);
                    }
                }
                lived += 10;
            }
        }.runTaskTimer(JavaPlugin.getProvidingPlugin(StrongholdDebug.class), 0L, 10L);
    }

    private void drawCuboidOutline(Player player, Location origin, PlacedRoom room, Color color) {
        var bounds = room.worldBounds();
        double minX = origin.getX() + bounds.min().x();
        double minY = origin.getY() + bounds.min().y() + 0.1;
        double minZ = origin.getZ() + bounds.min().z();
        double maxX = origin.getX() + bounds.max().x() + 1.0;
        double maxY = origin.getY() + bounds.max().y() + 1.0;
        double maxZ = origin.getZ() + bounds.max().z() + 1.0;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);

        Location p000 = new Location(origin.getWorld(), minX, minY, minZ);
        Location p001 = new Location(origin.getWorld(), minX, minY, maxZ);
        Location p010 = new Location(origin.getWorld(), minX, maxY, minZ);
        Location p011 = new Location(origin.getWorld(), minX, maxY, maxZ);
        Location p100 = new Location(origin.getWorld(), maxX, minY, minZ);
        Location p101 = new Location(origin.getWorld(), maxX, minY, maxZ);
        Location p110 = new Location(origin.getWorld(), maxX, maxY, minZ);
        Location p111 = new Location(origin.getWorld(), maxX, maxY, maxZ);

        drawLine(player, p000, p001, dust);
        drawLine(player, p000, p010, dust);
        drawLine(player, p000, p100, dust);
        drawLine(player, p111, p110, dust);
        drawLine(player, p111, p101, dust);
        drawLine(player, p111, p011, dust);
        drawLine(player, p001, p011, dust);
        drawLine(player, p001, p101, dust);
        drawLine(player, p010, p011, dust);
        drawLine(player, p010, p110, dust);
        drawLine(player, p100, p101, dust);
        drawLine(player, p100, p110, dust);
    }

    private void drawLine(Player player, Location start, Location end, Particle.DustOptions dust) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        int steps = Math.max(4, (int) Math.ceil(Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 2.0));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Location point = start.clone().add(dx * t, dy * t, dz * t);
            player.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, dust);
        }
    }
}

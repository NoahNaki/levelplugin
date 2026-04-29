package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MageBlinkSpell implements SpellHandler {
    private final Main plugin;
    private final double maxDistance;
    private final double momentumStrength;
    private final double maxUpwardMomentum;
    private final boolean fireNovaOnArrival;

    public MageBlinkSpell(Main plugin, double maxDistance) {
        this(plugin, maxDistance, 0.55, 0.50);
    }

    public MageBlinkSpell(Main plugin, double maxDistance, double momentumStrength, double maxUpwardMomentum) {
        this(plugin, maxDistance, momentumStrength, maxUpwardMomentum, false);
    }

    public MageBlinkSpell(Main plugin, double maxDistance, double momentumStrength, double maxUpwardMomentum, boolean fireNovaOnArrival) {
        this.plugin = plugin;
        this.maxDistance = maxDistance;
        this.momentumStrength = Math.max(0.0, momentumStrength);
        this.maxUpwardMomentum = Math.max(0.0, maxUpwardMomentum);
        this.fireNovaOnArrival = fireNovaOnArrival;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location destination = SpellTargetingUtil.resolveBlinkDestination(caster, maxDistance);
        if (destination == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No safe blink destination in sight.");
            return;
        }

        Location origin = caster.getLocation().clone();
        origin.getWorld().spawnParticle(Particle.PORTAL, origin.add(0.0, 1.0, 0.0), 35, 0.35, 0.5, 0.35, 0.18);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.55f, 1.45f);

        destination.setYaw(caster.getLocation().getYaw());
        destination.setPitch(caster.getLocation().getPitch());
        Vector blinkMomentum = computeBlinkMomentum(origin, destination);
        TeleportUtils.safeTeleport(caster, destination, false);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (blinkMomentum != null) {
                caster.setVelocity(blinkMomentum.clone());
            }
            Location landed = caster.getLocation().clone().add(0.0, 1.0, 0.0);
            caster.getWorld().spawnParticle(Particle.END_ROD, landed, 24, 0.4, 0.55, 0.4, 0.02);
            caster.getWorld().spawnParticle(Particle.PORTAL, landed, 42, 0.4, 0.55, 0.4, 0.21);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 1.55f);
            if (fireNovaOnArrival) {
                spawnFireSphere(landed);
            }
        }, 1L);
    }

    private void spawnFireSphere(Location center) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        double radius = 3.2;
        for (double theta = 0.0; theta < Math.PI; theta += Math.PI / 12.0) {
            for (double phi = 0.0; phi < Math.PI * 2.0; phi += Math.PI / 12.0) {
                double x = radius * Math.sin(theta) * Math.cos(phi);
                double y = radius * Math.cos(theta);
                double z = radius * Math.sin(theta) * Math.sin(phi);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, y, z), 1, 0, 0, 0, 0.01);
            }
        }
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0.15, 0.15, 0.15, 0.0);
    }

    private Vector computeBlinkMomentum(Location origin, Location destination) {
        if (origin == null || destination == null || momentumStrength <= 0.0) {
            return null;
        }
        Vector travel = destination.toVector().subtract(origin.toVector());
        if (travel.lengthSquared() <= 0.0001) {
            return null;
        }
        Vector launch = travel.normalize().multiply(momentumStrength);
        launch.setY(Math.min(maxUpwardMomentum, Math.max(-0.2, launch.getY())));
        return launch;
    }
}

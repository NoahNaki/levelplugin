package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.spells.ArcSlashCombatUtil;
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

public class RogueRazorDashSpell implements SpellHandler {
    private final double dashDistance;

    public RogueRazorDashSpell(double dashDistance) {
        this.dashDistance = dashDistance;
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location destination = SpellTargetingUtil.resolveBlinkDestination(caster, dashDistance);
        if (destination == null) {
            ChatMessageUtil.send(caster, ChatMessageUtil.MessageType.WARNING,
                    "No safe dash destination in sight.");
            return;
        }

        Location start = caster.getLocation().clone();
        Vector travel = destination.toVector().subtract(start.toVector());
        double length = travel.length();
        if (length <= 0.01) {
            return;
        }
        Vector dir = travel.clone().normalize();
        Location orientation = caster.getLocation().clone();
        orientation.setDirection(dir.clone());

        int slashes = Math.max(1, (int) Math.ceil(length / 1.8));
        for (int i = 1; i <= slashes; i++) {
            double progress = i / (double) slashes;
            Location impact = start.clone().add(travel.clone().multiply(progress)).add(0.0, 1.0, 0.0);
            ArcSlashCombatUtil.strike(caster, impact, orientation, Particle.CRIT, 3.8, 1.45);
        }

        caster.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.75f);
        destination.setYaw(caster.getLocation().getYaw());
        destination.setPitch(caster.getLocation().getPitch());
        TeleportUtils.safeTeleport(caster, destination, true);
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().clone().add(0.0, 1.0, 0.0),
                4, 0.25, 0.2, 0.25, 0.0);
    }
}

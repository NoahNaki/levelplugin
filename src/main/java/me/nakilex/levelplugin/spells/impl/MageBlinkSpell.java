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

public class MageBlinkSpell implements SpellHandler {
    private final Main plugin;
    private final double maxDistance;

    public MageBlinkSpell(Main plugin, double maxDistance) {
        this.plugin = plugin;
        this.maxDistance = maxDistance;
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
        TeleportUtils.safeTeleport(caster, destination);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location landed = caster.getLocation().clone().add(0.0, 1.0, 0.0);
            caster.getWorld().spawnParticle(Particle.END_ROD, landed, 24, 0.4, 0.55, 0.4, 0.02);
            caster.getWorld().spawnParticle(Particle.PORTAL, landed, 42, 0.4, 0.55, 0.4, 0.21);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 1.55f);
        }, 1L);
    }
}

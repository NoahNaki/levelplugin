package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.RayTraceResult;

/**
 * Quick teleport in the direction the player is facing.
 */
public class ShadowStepEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        RayTraceResult res = player.rayTraceBlocks(15);
        Location dest;
        if (res != null && res.getHitBlock() != null) {
            Block b = res.getHitBlock();
            dest = b.getLocation().add(0.5, 1, 0.5);
        } else {
            dest = player.getLocation().add(player.getLocation().getDirection().multiply(8));
        }
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.getWorld().spawnParticle(Particle.SQUID_INK, dest, 20, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }
}

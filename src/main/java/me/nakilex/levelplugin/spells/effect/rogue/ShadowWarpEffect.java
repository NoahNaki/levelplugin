package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Teleport forward leaving a short-lived decoy behind.
 */
public class ShadowWarpEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Location start = player.getLocation();
        Location dest = start.clone().add(start.getDirection().normalize().multiply(8));

        Block block = dest.getBlock();
        if (!block.isPassable()) {
            dest = block.getLocation().add(0.5, 1, 0.5);
        }

        // spawn a simple decoy using an armor stand with the player's head
        ArmorStand decoy = start.getWorld().spawn(start, ArmorStand.class);
        decoy.setInvisible(true);
        decoy.setMarker(true);
        decoy.setGravity(false);
        decoy.setBasePlate(false);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        decoy.getEquipment().setHelmet(head);

        start.getWorld().spawnParticle(Particle.LARGE_SMOKE, start, 20, 0.5,0.5,0.5,0.1);
        start.getWorld().playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
        dest.getWorld().spawnParticle(Particle.SQUID_INK, dest, 20, 0.5,0.5,0.5,0.1);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);

        new BukkitRunnable() {
            int life = 0;
            @Override
            public void run() {
                if (life++ >= 40 || !decoy.isValid()) {
                    decoy.remove();
                    cancel();
                    return;
                }
                decoy.getWorld().spawnParticle(Particle.SMOKE_NORMAL, decoy.getLocation(), 5, 0.2,0.2,0.2,0.01);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }
}

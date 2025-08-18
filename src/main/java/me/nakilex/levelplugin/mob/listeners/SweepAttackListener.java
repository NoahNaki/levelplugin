package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Adds a small area sweep to sword and shovel basic attacks.
 * When a player lands a hit with one of these weapons, nearby
 * mobs also take damage as if struck by the same attack.
 */
public class SweepAttackListener implements Listener {
    public static final String SWEEP_META = "SweepAttack";

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null) return;
        Material type = weapon.getType();
        String name = type.name();
        if (!name.endsWith("_SWORD") && !name.endsWith("_SHOVEL")) return;

        double baseDamage = event.getDamage();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 1);

        double radius = 3.0;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e.equals(event.getEntity())) continue;
            if (e instanceof Player) continue;

            player.setMetadata(SWEEP_META, new FixedMetadataValue(Main.getInstance(), true));
            le.damage(baseDamage, player);
            player.removeMetadata(SWEEP_META, Main.getInstance());
        }
    }
}

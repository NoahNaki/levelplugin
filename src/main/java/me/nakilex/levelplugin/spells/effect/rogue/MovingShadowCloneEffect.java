package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.utils.MetadataTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Shadow Clone variant that actively moves and attacks nearby targets.
 */
public class MovingShadowCloneEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID id = player.getUniqueId();
        NPC clone = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Clone");
        clone.spawn(player.getLocation());
        clone.getOrAddTrait(MetadataTrait.class).setOwner(id);
        clone.data().setPersistent("player-skin-name", player.getName());
        if (clone.getEntity() instanceof Player p) {
            p.getInventory().setArmorContents(player.getInventory().getArmorContents());
            p.getInventory().setItemInMainHand(new ItemStack(player.getInventory().getItemInMainHand()));
        }

        new BukkitRunnable() {
            int life = 200;
            @Override
            public void run() {
                if (!clone.isSpawned() || !player.isOnline()) { cancel(); return; }
                if (life-- <= 0) {
                    destroy();
                    cancel();
                    return;
                }
                LivingEntity target = null;
                double best = Double.MAX_VALUE;
                Location loc = clone.getEntity().getLocation();
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 8,8,8)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    double d = le.getLocation().distanceSquared(loc);
                    if (d < best) { best = d; target = le; }
                }
                if (target != null) {
                    clone.faceLocation(target.getLocation());
                    clone.getNavigator().setTarget(target, true);
                    if (target.getLocation().distanceSquared(loc) < 4) {
                        double dmg = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
                        SpellUtils.dealWithChat(player, target, dmg, "Shadow Clone");
                        loc.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.3,0.3,0.3);
                    }
                }
            }
            private void destroy() {
                Location l = clone.getEntity().getLocation();
                clone.despawn();
                clone.destroy();
                l.getWorld().spawnParticle(Particle.EXPLOSION, l, 1);
                l.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}

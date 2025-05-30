// src/main/java/me/nakilex/levelplugin/spells/effect/archer/BowDroneEffect.java
package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class BowDroneEffect implements SpellEffect {
    private static final String META_KEY = "ArcherSpell";

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID pid = player.getUniqueId();
        var active = Main.getInstance().getActiveBowDrones();

        // 1) Prevent more than one drone per player
        if (active.containsKey(pid)) {
            player.sendMessage(ChatColor.RED + "You already have a Sentry active!");
            return;
        }

        // 2) Spawn the NPC “drone” above the player
        Location spawnLoc = player.getLocation().add(2, 3, 2);
        NPC drone = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "");
        drone.spawn(spawnLoc);
        active.put(pid, drone);

        // 3) Make it invisible, glowing & equip a loaded Crossbow
        LivingEntity ent = (LivingEntity) drone.getEntity();
        ent.setInvisible(true);
        ent.setGlowing(false);
        ent.setInvulnerable(true);
        ItemStack cb = new ItemStack(Material.CROSSBOW);
        CrossbowMeta cbMeta = (CrossbowMeta) cb.getItemMeta();
        cbMeta.addChargedProjectile(new ItemStack(Material.ARROW));
        cb.setItemMeta(cbMeta);
        ent.getEquipment().setItemInMainHand(cb);

        // 4) Visual-effect task: swirling particles
        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                if (!drone.isSpawned() || !player.isOnline()) {
                    active.remove(pid);
                    cancel();
                    return;
                }
                Location center = ent.getLocation().clone().add(0, 1, 0);
                double radius = 1.2;

                // smoke on one side
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(x, 0, z), 2, 0, 0, 0, 0);
                // crit on the opposite
                center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(-x, 0, -z), 2, 0, 0, 0, 0);

                angle += Math.PI / 16;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        // 5) Follow-task: keep hovering above the player
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!drone.isSpawned() || !player.isOnline()) {
                    active.remove(pid);
                    cancel();
                    return;
                }
                drone.teleport(player.getLocation().add(2, 3, 2), PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        // 6) Shooting task: find targets & fire arrows
        new BukkitRunnable() {
            int ticks = 0;
            final int maxLife = 20;

            @Override
            public void run() {
                if (++ticks > maxLife || !drone.isSpawned() || !player.isOnline()) {
                    drone.despawn();
                    drone.destroy();
                    active.remove(pid);
                    cancel();
                    return;
                }

                Location loc = ent.getEyeLocation().clone();
                LivingEntity target = null;
                double bestDist = Double.MAX_VALUE;

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 15, 15, 15)) {
                    if (!(e instanceof LivingEntity le)) continue;
                    if (le.equals(player)) continue;
                    if (le instanceof Player p && !DuelManager.getInstance().areInDuel(pid, p.getUniqueId())) continue;

                    double d = le.getLocation().distanceSquared(loc);
                    if (d < bestDist) { bestDist = d; target = le; }
                }
                if (target == null) return;

                Arrow shot = loc.getWorld().spawnArrow(
                    loc,
                    target.getEyeLocation().toVector().subtract(loc.toVector()).normalize(),
                    3.0f, 0.0f
                );
                shot.setShooter(player);
                shot.setMetadata(META_KEY, new FixedMetadataValue(Main.getInstance(), pid));
                loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_SHOOT, 0.7f, 1f);

                // collision + damage-chat
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!shot.isValid() || shot.isDead()) { cancel(); return; }
                        for (Entity near : shot.getNearbyEntities(1, 1, 1)) {
                            if (!(near instanceof LivingEntity le)) continue;
                            if (le.equals(player)) continue;
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(pid, p.getUniqueId())) continue;

                            SpellUtils.dealWithChat(
                                player, le,
                                player.getAttribute(Attribute.ATTACK_DAMAGE).getValue(),
                                "Bow Drone"
                            );
                            shot.remove();
                            cancel();
                            return;
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 1L);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }
}

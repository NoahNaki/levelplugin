package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.utils.MetadataTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class RogueSpell {

    public void castRogueSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "SHADOW_STEP":
                castShadowStep(player);
                break;
            case "BLADE_FURY":
                castBladeFury(player);
                break;
            case "VANISH":
                castShurikenThrow(player);
                break;
            case "DAGGER_THROW":
                castShadowClone(player);
                break;
            default:
                player.sendMessage("§eUnknown Rogue Spell: " + effectKey);
        }
    }

    private void castShurikenThrow(Player player) {
        // 1) compute damage once
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.3;

        // 2) setup
        Location center = player.getLocation().clone();
        Vector forwardVelocity = center.getDirection().multiply(1.3);
        List<ArmorStand> stands = new ArrayList<>();
        double radius = 0.2;
        Vector[] offsets = {
            new Vector(radius, 0, 0), new Vector(-radius, 0, 0),
            new Vector(0, 0, radius), new Vector(0, 0, -radius)
        };

        // 3) spawn the rotating “shuriken” armor stands
        for (Vector offset : offsets) {
            Location loc = center.clone().add(offset);
            ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setArms(true);
            stand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            stand.setRightArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(90)));
            stand.setLeftArmPose(new EulerAngle(Math.toRadians(270), 0, Math.toRadians(-90)));
            stands.add(stand);
        }

        // 4) animate & detect impact
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (ticks++ >= 60) {
                    player.sendMessage("§eYour shuriken dissipated!");
                    removeAll(stands);
                    cancel();
                    return;
                }

                angle += Math.toRadians(45);
                center.add(forwardVelocity);

                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand stand = stands.get(i);
                    Vector off = offsets[i].clone();
                    double cos = Math.cos(angle), sin = Math.sin(angle);
                    double x = off.getX() * cos - off.getZ() * sin;
                    double z = off.getX() * sin + off.getZ() * cos;
                    Location loc = center.clone().add(x, 0, z);

                    // 4a) block collision → teleport player there
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR && !block.isPassable()) {
                        player.teleport(loc);
                        player.sendMessage("§aYou teleported to the shuriken’s location!");
                        removeAll(stands);
                        cancel();
                        return;
                    }

                    // 4b) entity collision → deal damage + chat, then explode
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.3, 0.3, 0.3)) {
                        if (e.equals(player) || stands.contains(e)) continue;
                        if (e instanceof LivingEntity le) {
                            // —— damage + chat here ——
                            SpellUtils.dealWithChat(player, le, damage, "Shuriken Throw");

                            // then explosion & cleanup
                            loc.getWorld().createExplosion(loc, 2f, false, false);
                            player.sendMessage("§cYour shuriken exploded on impact!");
                            removeAll(stands);
                            cancel();
                            return;
                        }
                    }

                    // 4c) move the stand
                    loc.setYaw((float) Math.toDegrees(Math.atan2(-x, z)));
                    stand.teleport(loc);
                }
            }

            private void removeAll(List<ArmorStand> list) {
                list.forEach(s -> { if (!s.isDead()) s.remove(); });
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


    private void castShadowClone(Player player) {
        UUID id = player.getUniqueId();

        // 1) Swap with existing clone if there is one
        NPC existing = null;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(MetadataTrait.class)
                && npc.getTrait(MetadataTrait.class).getOwner().equals(id)) {
                existing = npc;
                break;
            }
        }
        if (existing != null) {
            Location cloneLoc = existing.getEntity().getLocation();
            existing.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.teleport(cloneLoc);
            player.sendMessage("§aYou swapped places with your shadow clone!");
            player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1f, 1f
            );
            return;
        }

        // 2) Spawn a brand-new clone
        NPC clone = CitizensAPI.getNPCRegistry()
            .createNPC(EntityType.PLAYER, "Shadow Clone");
        clone.spawn(player.getLocation());
        clone.getOrAddTrait(MetadataTrait.class).setOwner(id);
        clone.data().setPersistent("player-skin-name", player.getName());
        if (clone.getEntity() instanceof Player p) {
            p.getInventory().setArmorContents(player.getInventory().getArmorContents());
            p.getInventory().setItemInMainHand(player.getInventory().getItemInMainHand());
            p.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
        }
        player.sendMessage("§aYou created a shadow clone!");

        // 3) Schedule its “explosion” in 5s
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!clone.isSpawned()) return;

                // a) Grab location, then kill the NPC
                Location loc = clone.getEntity().getLocation();
                clone.despawn();
                clone.destroy();

                // b) Visual + sound effect ONLY
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                loc.getWorld().playSound(
                    loc,
                    Sound.ENTITY_GENERIC_EXPLODE,
                    1f, 1f
                );
                player.sendMessage("§cYour shadow clone exploded!");

                // c) Now *manually* damage every nearby target and log it
                double damage = player
                    .getAttribute(Attribute.ATTACK_DAMAGE)
                    .getValue() * 1.5;

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                    if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                    SpellUtils.dealWithChat(player, le, damage, "Shadow Clone");
                }
            }
        }.runTaskLater(
            Bukkit.getPluginManager().getPlugin("LevelPlugin"),
            100L
        );
    }



    private void castShadowStep(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 30, 0.5, 1, 0.5);

        new BukkitRunnable() {
            int casts = 0;

            @Override
            public void run() {
                if (casts++ >= 5) { cancel(); return; }

                LivingEntity target = player.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15).stream()
                    .filter(e -> e instanceof LivingEntity && e != player)
                    .map(e -> (LivingEntity) e)
                    .filter(e -> !(e instanceof Player) || DuelManager.getInstance().areInDuel(player.getUniqueId(), ((Player) e).getUniqueId()))
                    .min(Comparator.comparingDouble(e -> e.getLocation().distance(player.getLocation())))
                    .orElse(null);

                if (target != null) {
                    Location behind = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1).normalize());
                    behind.setYaw(target.getLocation().getYaw());
                    behind.setPitch(target.getLocation().getPitch());
                    player.teleport(behind);

                    double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;
                    SpellUtils.dealWithChat(player, target, damage, "Shadow Step");

                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 30, 0.5, 1, 0.5);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation(), 10, 0.5, 0.5, 0.5);
                    target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 20, 0.5, 1, 0.5);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.2f);
                    player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

                    if (target instanceof Player) {
                        ((Player) target).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                    }
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 10, 0.3, 0.3, 0.3);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0));
                }

                player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 10, 0.3, 0.3, 0.3);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0, 10);
    }

    private void castBladeFury(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 30, radius, 1, radius);

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (e instanceof LivingEntity le && le != player) {
                SpellUtils.dealWithChat(player, le, damage, "Blade Fury");
            }
        }
    }
}

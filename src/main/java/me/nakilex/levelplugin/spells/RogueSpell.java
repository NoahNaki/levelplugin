package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
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
import org.bukkit.inventory.meta.FireworkMeta;
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
                castExecute(player);
                break;
            case "BLADE_FURY":
                castBladeFury(player);
                break;
            case "VANISH":
                castVanish(player);
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

    /**
     * Makes the player invisible for 5 seconds, blinks them forward ~3 blocks,
     * and adds a swirly pre-vanish ring, multi-layered particles, firework burst,
     * sound layers, after-blink trail, and brief glow on arrival.
     */
    private void castVanish(Player player) {
        Location origin = player.getLocation();
        World world   = origin.getWorld();
        final int BASE_DURATION = 100;   // 5 seconds in ticks
        final int EXTEND_DURATION = 20;  // 2 seconds

        // Determine current effect state
        boolean hasInvis = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
        int newDuration = BASE_DURATION;
        int speedAmp = 0;
        int jumpAmp = 0;

        if (hasInvis) {
            PotionEffect invis = player.getPotionEffect(PotionEffectType.INVISIBILITY);
            newDuration = invis.getDuration() + EXTEND_DURATION;

            PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
            PotionEffect jump  = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
            speedAmp = (speed  != null ? speed.getAmplifier() : 0) + 1;
            jumpAmp  = (jump   != null ? jump.getAmplifier()  : 0) + 1;
        }

        // 1) Pre-vanish swirling ring
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick++ > 15) { cancel(); return; }
                double radius = 1.0 + tick * 0.05;
                for (double ang = 0; ang < 360; ang += 20) {
                    double rad = Math.toRadians(ang + tick * 20);
                    Location point = origin.clone().add(
                        Math.cos(rad) * radius,
                        0.5,
                        Math.sin(rad) * radius
                    );
                    world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 1);

        // 2) Apply invisibility, speed, and jump
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, newDuration, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       newDuration, speedAmp, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,        newDuration, jumpAmp,  false, false));

        // **3) Hide the player from all others (not vanish offline!)**
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hidePlayer(Main.getInstance(), player);
            }
        }

        // 4) Layered cast sounds
        world.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        world.playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL,  1f, 1.2f);
        world.playSound(origin, Sound.ITEM_TOTEM_USE,                0.8f, 1f);

        // 5) Particle burst
        world.spawnParticle(Particle.FIREWORK, origin, 30, 1, 1, 1, 0.05);
        world.spawnParticle(Particle.SMOKE,      origin, 20, 0.5, 1, 0.5, 0.02);

        // **6) After duration, show player again**
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(player)) {
                        other.showPlayer(Main.getInstance(), player);
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), newDuration);
    }


    private void castExecute(Player player) {
        // Locate first valid target
        LivingEntity found = null;
        for (Entity e : player.getWorld().getNearbyEntities(player.getEyeLocation(), 10, 10, 10)) {
            if (e instanceof LivingEntity && !e.equals(player)) {
                found = (LivingEntity) e;
                break;
            }
        }
        if (found == null) {
            player.sendMessage("§cNo valid target in range!");
            return;
        }
        final LivingEntity target = found;
        final World world = target.getWorld();
        final double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;

        // Prepare strike directions (8 around + above + below)
        final Vector[] directions = new Vector[10];
        int idx = 0;
        // horizontal circle
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            directions[idx++] = new Vector(Math.cos(angle), 0.2, Math.sin(angle));
        }
        // above strike
        directions[idx++] = new Vector(0, -1, 0);
        // below strike
        directions[idx++] = new Vector(0, 1, 0);

        // Schedule sequential strikes
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= directions.length) {
                    // final damage and finish
                    SpellUtils.dealWithChat(player, target, damage, "Execute");
                    cancel();
                    return;
                }
                // Apply velocity
                Vector v = directions[step].clone().multiply(1.2);
                target.setVelocity(v);
                // VFX/SFX
                world.spawnParticle(Particle.CRIT, target.getLocation(), 15, 0.5, 1, 0.5);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);

        player.sendMessage("§aYou unleash a thousand cuts upon your foe!");
    }

}

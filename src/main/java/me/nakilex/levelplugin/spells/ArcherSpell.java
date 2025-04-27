package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArcherSpell {

    private static final String META_KEY = "ArcherSpell";          // <— new
    private final Main plugin = Main.getInstance();

    public void castArcherSpell(Player player, String effectKey) {
        switch (effectKey.toUpperCase()) {
            case "POWER_SHOT":
                castPowerShot(player);
                break;
            case "ARROW_STORM":
                castArrowStorm(player);
                break;
            case "EXPLOSIVE_ARROW":
                castExplosiveArrow(player);
                break;
            case "GRAPPLE_HOOK":
                castGrappleHook(player);
                break;
            default:
                player.sendMessage("§eUnknown Archer Spell: " + effectKey);
                break;
        }
    }

    private void castPowerShot(Player player) {
        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.8; // 80% weapon damage per arrow
        int duration = 100;
        int arrowCount = 30;

        Location targetLocation = player.getTargetBlockExact(20) != null
            ? player.getTargetBlockExact(20).getLocation().add(0.5, 0.5, 0.5)
            : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(5));

        player.getWorld().playSound(targetLocation, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        new BukkitRunnable() {
            int arrowsSpawned = 0;

            @Override
            public void run() {
                if (arrowsSpawned >= arrowCount) {
                    cancel();
                    return;
                }

                double xOffset = (Math.random() - 0.5) * 2 * radius;
                double zOffset = (Math.random() - 0.5) * 2 * radius;
                Location dropLocation = targetLocation.clone().add(xOffset, 15, zOffset);

                dropLocation.getWorld().spawnParticle(Particle.CLOUD, dropLocation, 10, 0.3, 0.3, 0.3, 0.02);
                dropLocation.getWorld().playSound(dropLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);

                Arrow arrow = player.getWorld().spawnArrow(dropLocation, new Vector(0, -3, 0), 1.5f, 0.0f);
                arrow.setCustomName("ArrowRain");
                arrow.setCustomNameVisible(false);
                arrow.setShooter(player);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

                arrow.setMetadata(META_KEY, new FixedMetadataValue(plugin, player.getUniqueId()));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isDead()) {
                            cancel();
                            return;
                        }

                        for (Entity entity : arrow.getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity && entity != player) {
                                if (entity instanceof Player p
                                    && !DuelManager.getInstance()
                                    .areInDuel(player.getUniqueId(), p.getUniqueId())) {
                                    continue;
                                }
                                LivingEntity target = (LivingEntity) entity;

                                // —— damage + chat here ——
                                SpellUtils.dealWithChat(player, target, damage, "Power Shot");

                                // slowness effect
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                            }
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 1L);

                arrowsSpawned++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, duration / arrowCount);
    }



    private void castExplosiveArrow(Player player) {

        double explosionRadius = 5.0;
        double damage          = player.getAttribute(Attribute.ATTACK_DAMAGE)
            .getValue() * 2.0;

        player.getWorld().playSound(player.getLocation(),
            Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(player.getLocation().getDirection().multiply(2));
        arrow.setCustomName("ExplosiveArrow");
        arrow.setCustomNameVisible(false);
        arrow.setCritical(true);

        // duel-filter metadata
        arrow.setMetadata(META_KEY,
            new FixedMetadataValue(plugin, player.getUniqueId()));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    cancel();
                    return;
                }

                arrow.getWorld().spawnParticle(Particle.CRIT,
                    arrow.getLocation(), 5, 0.1, 0.1, 0.1);

                /* ── explode when the arrow sticks ───────────────── */
                if (arrow.isOnGround()
                    || !arrow.getLocation().getBlock().isPassable()) {

                    Location boom = arrow.getLocation();
                    World    w    = boom.getWorld();

                    /* ✨ purely-visual blast ✨ */
                    w.spawnParticle(Particle.EXPLOSION,       boom, 1);
                    w.spawnParticle(Particle.FIREWORK, boom, 100, 0.8, 0.8, 0.8, 0.05);
                    w.playSound(boom, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                    /* ✔ duel-safe manual damage loop */
                    for (Entity e : w.getNearbyEntities(boom,
                        explosionRadius,
                        explosionRadius,
                        explosionRadius)) {

                        if (!(e instanceof LivingEntity le) || le == player) continue;
                        if (!canHit(player, e))                        continue;

                        SpellUtils.dealWithChat(player, le, damage, "Explosive Arrow");
                        le.setFireTicks(60);
                    }

                    arrow.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    private final Set<UUID> grappleCooldown = new HashSet<>();

    private boolean castGrappleHook(Player player) {
        // 1) Must be standing on ground to fire
        if (!player.isOnGround()) {
            player.sendMessage(ChatColor.RED + "You must land before you can use Grapple Hook again!");
            return false;
        }

        // 2) Must not already be recharging
        UUID id = player.getUniqueId();
        if (grappleCooldown.contains(id)) {
            player.sendMessage(ChatColor.RED + "Grapple Hook is still recharging...");
            return false;
        }

        // 3) Mark on cooldown
        grappleCooldown.add(id);

        // 4) Now fire your snowball hook
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getLocation().getDirection().multiply(2));
        projectile.setCustomName("GrappleHook");
        projectile.setCustomNameVisible(false);


        projectile.setMetadata(META_KEY, new FixedMetadataValue(plugin, player.getUniqueId()));

        // Glide handler will clear cooldown once they land
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }

                projectile.getWorld().spawnParticle(Particle.WITCH, projectile.getLocation(), 5, 0.1, 0.1, 0.1);

                if (!projectile.getNearbyEntities(1, 1, 1).isEmpty()
                    || projectile.getLocation().getBlock().getType() != Material.AIR) {

                    // start pulling them
                    Location hookLoc = projectile.getLocation();
                    Vector pull = hookLoc.toVector().subtract(player.getLocation().toVector())
                        .normalize().multiply(1.5);
                    player.setVelocity(pull.add(new Vector(0, 0.5, 0)));

                    handleGlideAndSlam(player);
                    projectile.remove();
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        return true;
    }

    private void handleGlideAndSlam(Player player) {
        new BukkitRunnable() {
            boolean slamTriggered = false;

            @Override
            public void run() {
                // As soon as they’re back on ground, clear the cooldown
                if (player.isOnGround()) {
                    grappleCooldown.remove(player.getUniqueId());
                    if (slamTriggered) {
                        performSlam(player);
                    }
                    cancel();
                    return;
                }

                // While airborne, allow a sneak to slam
                if (slamTriggered) {
                    Vector vel = player.getVelocity();
                    vel.setY(Math.max(vel.getY() - 0.5, -2.5));
                    player.setVelocity(vel);
                } else {
                    Vector glide = player.getVelocity().multiply(0.9);
                    glide.setY(Math.max(player.getVelocity().getY() - 0.05, -0.1));
                    player.setVelocity(glide);

                    if (player.isSneaking()) {
                        slamTriggered = true;
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.8f);
                        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 1, 0.5);
                        player.setVelocity(new Vector(0, -2, 0));
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 10L, 1L);
    }

    /** Grapple-Hook landing slam */
    private void performSlam(Player player) {

        double radius = 5.0;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2.0;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20);

        for (Entity entity : player.getWorld()
            .getNearbyEntities(player.getLocation(),
                radius, radius, radius)) {

            if (!(entity instanceof LivingEntity le) || le == player) continue;
            if (!canHit(player, le)) continue;        // ░░ NEW GUARD ░░

            /* — damage + chat — */
            SpellUtils.dealWithChat(player, le, damage, "Grapple Hook");

            /* — knock-back only when duel-legal — */
            Vector kb = le.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .normalize().multiply(1.5);
            kb.setY(0.5);
            le.setVelocity(kb);
        }
    }


    private void castArrowStorm(Player player) {
        int arrowCount = 10;
        double spread = 0.1;
        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 0.5; // 50% weapon damage per arrow

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        new BukkitRunnable() {
            int arrowsFired = 0;

            @Override
            public void run() {
                if (arrowsFired >= arrowCount) {
                    cancel();
                    return;
                }

                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setDamage(0);  // prevent default damage
                Vector dir = player.getLocation().getDirection().clone();
                dir.add(new Vector((Math.random()-0.5)*spread, (Math.random()-0.5)*spread, (Math.random()-0.5)*spread));
                arrow.setVelocity(dir.multiply(2));
                arrow.setCustomName("ArrowStorm");
                arrow.setCustomNameVisible(false);
                arrow.setCritical(true);

                arrow.setMetadata(META_KEY, new FixedMetadataValue(plugin, player.getUniqueId()));


                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isDead()) {
                            cancel();
                            return;
                        }

                        arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 5, 0.1, 0.1, 0.1);

                        for (Entity entity : arrow.getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof LivingEntity le && le != player) {
                                if (entity instanceof Player p
                                    && !DuelManager.getInstance()
                                    .areInDuel(player.getUniqueId(), p.getUniqueId())) {
                                    continue;
                                }
                                // —— damage + chat here ——
                                SpellUtils.dealWithChat(player, le, damage, "Arrow Storm");
                                arrow.remove();
                                cancel();
                                return;
                            }
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 1L);

                arrowsFired++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);
    }

    /** true = we are allowed to hurt that target */
    private boolean canHit(Player caster, Entity target) {
        return !(target instanceof Player p)           // mobs are always OK
            || DuelManager.getInstance()
            .areInDuel(caster.getUniqueId(), p.getUniqueId());
    }


}

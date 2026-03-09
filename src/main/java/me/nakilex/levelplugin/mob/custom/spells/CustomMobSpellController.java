package me.nakilex.levelplugin.mob.custom.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import me.nakilex.levelplugin.mob.custom.CustomMobInstance;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reusable runtime for custom mob spells declared in custom mob YAML definitions.
 */
public class CustomMobSpellController {
    private static final String SPELL_MAGE_FIREBALL_BASIC = "mage_fireball_basic";
    private static final double HIT_RADIUS = 0.45;

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CustomMobSpellController(Main plugin, CustomMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        startTicker();
    }

    private void startTicker() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 2L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (CustomMobInstance instance : mobManager.getActiveMobs().values()) {
            LivingEntity entity = instance.entity();
            if (!(entity instanceof Mob mob) || mob.isDead()) {
                continue;
            }
            if (!(mob.getTarget() instanceof Player target) || target.isDead()) {
                continue;
            }
            for (CustomMobDefinition.CustomMobSpell spell : instance.definition().spells()) {
                if (!isReady(instance.entity().getUniqueId(), spell, now)) {
                    continue;
                }
                if (mob.getLocation().distanceSquared(target.getLocation()) > spell.range() * spell.range()) {
                    continue;
                }
                castSpell(instance, mob, target, spell);
                markCast(instance.entity().getUniqueId(), spell, now);
            }
        }
    }

    private boolean isReady(UUID mobId, CustomMobDefinition.CustomMobSpell spell, long now) {
        long nextAllowed = cooldowns.getOrDefault(mobId, Map.of()).getOrDefault(spell.id(), 0L);
        return now >= nextAllowed;
    }

    private void markCast(UUID mobId, CustomMobDefinition.CustomMobSpell spell, long now) {
        cooldowns.computeIfAbsent(mobId, ignored -> new HashMap<>())
                .put(spell.id(), now + (spell.intervalTicks() * 50L));
    }

    public void clearMob(UUID mobId) {
        cooldowns.remove(mobId);
    }

    private void castSpell(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        if (SPELL_MAGE_FIREBALL_BASIC.equals(spell.id())) {
            castMageFireball(instance, caster, target, spell);
        }
    }

    private void castMageFireball(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        Location eye = caster.getEyeLocation().clone();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        MageFireballBasicAttackSpell.FireballSpawnResult spawnResult =
                MageFireballBasicAttackSpell.spawnProjectileAnchor(plugin, eye, direction);
        if (spawnResult == null) {
            return;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.65f, 0.9f);
        launchProjectile(instance, caster, target, spawnResult.anchor(), direction.normalize(), spell);
    }

    private void launchProjectile(CustomMobInstance instance,
                                  Mob caster,
                                  Player intendedTarget,
                                  ArmorStand projectile,
                                  Vector direction,
                                  CustomMobDefinition.CustomMobSpell spell) {
        Vector step = direction.clone().multiply(spell.speed());
        double maxDistanceSq = spell.range() * spell.range();
        Location origin = projectile.getLocation().clone();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || caster.isDead() || intendedTarget.isDead()) {
                    removeProjectile(projectile);
                    cancel();
                    return;
                }
                Location current = projectile.getLocation();
                if (current.distanceSquared(origin) >= maxDistanceSq) {
                    removeProjectile(projectile);
                    cancel();
                    return;
                }

                Location next = current.clone().add(step);
                projectile.teleport(next);
                ModelEngineUtil.orientEntityToVector(projectile, step);
                SpellEffectUtil.spawnFireProjectileTrail(next);

                LivingEntity hit = findTargetAt(next, caster, projectile);
                if (hit != null) {
                    onImpact(caster, next, hit, spell);
                    removeProjectile(projectile);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private LivingEntity findTargetAt(Location center, Mob caster, ArmorStand projectile) {
        for (var entity : center.getWorld().getNearbyEntities(center, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDead() || living.equals(caster) || living.equals(projectile) || living instanceof ArmorStand) {
                continue;
            }
            return living;
        }
        return null;
    }

    private void onImpact(Mob caster, Location impact, LivingEntity target, CustomMobDefinition.CustomMobSpell spell) {
        SpellEffectUtil.spawnFireImpactEffect(impact);
        impact.getWorld().playSound(impact, Sound.BLOCK_FIRE_EXTINGUISH, 0.85f, 0.75f);
        target.damage(spell.damage(), caster);
        if (spell.burnTicks() > 0) {
            target.setFireTicks(Math.max(target.getFireTicks(), spell.burnTicks()));
        }
    }

    private void removeProjectile(ArmorStand projectile) {
        if (projectile.isValid()) {
            projectile.remove();
        }
    }
}

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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generic runtime for custom mob spells with weighted group selection + YAML scripts.
 */
public class CustomMobSpellController {
    private static final String SPELL_MAGE_FIREBALL_BASIC = "mage_fireball_basic";
    private static final String SPELL_RANGED_ARROW_BASIC = "ranged_arrow_basic";
    private static final double HIT_RADIUS = 0.45;

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final Random random = new Random();
    private final CustomMobSpellScriptEngine scriptEngine;
    private final Map<UUID, Map<String, Long>> spellCooldowns = new HashMap<>();
    private final Map<UUID, Long> globalCooldowns = new HashMap<>();

    public CustomMobSpellController(Main plugin, CustomMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.scriptEngine = new CustomMobSpellScriptEngine(plugin);
        startTicker();
    }

    public void reload() {
        scriptEngine.reload();
    }

    public void clearMob(UUID mobId) {
        spellCooldowns.remove(mobId);
        globalCooldowns.remove(mobId);
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
            if (!isGlobalReady(entity.getUniqueId(), now)) {
                continue;
            }
            maintainPreferredDistance(instance, mob, target);

            Map<String, List<CustomMobDefinition.CustomMobSpell>> byGroup = collectEligibleByGroup(instance, mob, target, now);
            for (List<CustomMobDefinition.CustomMobSpell> groupSpells : byGroup.values()) {
                CustomMobDefinition.CustomMobSpell selected = weightedPick(groupSpells);
                if (selected == null) {
                    continue;
                }
                castSpell(instance, mob, target, selected);
                markCast(entity.getUniqueId(), selected, now);
            }
        }
    }

    private void maintainPreferredDistance(CustomMobInstance instance, Mob mob, Player target) {
        List<CustomMobDefinition.CustomMobSpell> rangedSpells = instance.definition().spells().stream()
                .filter(spell -> spell.maxRange() >= 10.0 && spell.minRange() > 1.5)
                .toList();
        if (rangedSpells.isEmpty()) {
            return;
        }
        double preferredMin = rangedSpells.stream()
                .mapToDouble(CustomMobDefinition.CustomMobSpell::minRange)
                .average()
                .orElse(0.0);
        double preferredMax = rangedSpells.stream()
                .mapToDouble(CustomMobDefinition.CustomMobSpell::maxRange)
                .average()
                .orElse(0.0);
        double distance = mob.getLocation().distance(target.getLocation());
        Location mobLoc = mob.getLocation();
        Location targetLoc = target.getLocation();
        if (distance < preferredMin) {
            Vector away = mobLoc.toVector().subtract(targetLoc.toVector());
            if (away.lengthSquared() > 0.0001) {
                Vector push = away.normalize().multiply(0.35);
                push.setY(Math.max(0.05, mob.getVelocity().getY()));
                mob.setVelocity(push);
            }
        } else if (distance > preferredMax + 1.0) {
            Vector toward = targetLoc.toVector().subtract(mobLoc.toVector());
            if (toward.lengthSquared() > 0.0001) {
                Vector nudge = toward.normalize().multiply(0.18);
                nudge.setY(mob.getVelocity().getY());
                mob.setVelocity(mob.getVelocity().add(nudge));
            }
        }
    }

    private Map<String, List<CustomMobDefinition.CustomMobSpell>> collectEligibleByGroup(CustomMobInstance instance,
                                                                                          Mob mob,
                                                                                          Player target,
                                                                                          long now) {
        Map<String, List<CustomMobDefinition.CustomMobSpell>> grouped = new HashMap<>();
        double distance = mob.getLocation().distance(target.getLocation());
        for (CustomMobDefinition.CustomMobSpell spell : instance.definition().spells()) {
            if (!isSpellReady(mob.getUniqueId(), spell, now)) {
                continue;
            }
            if (distance < spell.minRange() || distance > spell.maxRange()) {
                continue;
            }
            if (spell.requireLineOfSight() && !mob.hasLineOfSight(target)) {
                continue;
            }
            grouped.computeIfAbsent(spell.selectionGroup(), k -> new ArrayList<>()).add(spell);
        }
        return grouped;
    }

    private boolean isSpellReady(UUID mobId, CustomMobDefinition.CustomMobSpell spell, long now) {
        long nextAllowed = spellCooldowns.getOrDefault(mobId, Map.of()).getOrDefault(spell.id(), 0L);
        return now >= nextAllowed;
    }

    private boolean isGlobalReady(UUID mobId, long now) {
        return now >= globalCooldowns.getOrDefault(mobId, 0L);
    }

    private void markCast(UUID mobId, CustomMobDefinition.CustomMobSpell spell, long now) {
        spellCooldowns.computeIfAbsent(mobId, ignored -> new HashMap<>())
                .put(spell.id(), now + (spell.intervalTicks() * 50L));
        if (spell.gcdTicks() > 0) {
            globalCooldowns.put(mobId, now + spell.gcdTicks() * 50L);
        }
    }

    private CustomMobDefinition.CustomMobSpell weightedPick(List<CustomMobDefinition.CustomMobSpell> spells) {
        if (spells.isEmpty()) {
            return null;
        }
        double totalWeight = spells.stream().mapToDouble(s -> Math.max(0.0, s.selectionWeight())).sum();
        if (totalWeight <= 0.0001) {
            return spells.get(random.nextInt(spells.size()));
        }
        double roll = random.nextDouble() * totalWeight;
        double cursor = 0;
        for (CustomMobDefinition.CustomMobSpell spell : spells) {
            cursor += Math.max(0.0, spell.selectionWeight());
            if (roll <= cursor) {
                return spell;
            }
        }
        return spells.get(spells.size() - 1);
    }

    private void castSpell(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        CustomMobSpellScriptEngine.SpellScript script = scriptEngine.getScript(spell.scriptKey());
        if (script != null && !script.actions().isEmpty()) {
            executeScript(instance, caster, target, spell, script);
            return;
        }
        castLegacyFallback(instance, caster, target, spell);
    }

    private void executeScript(CustomMobInstance instance,
                               Mob caster,
                               Player target,
                               CustomMobDefinition.CustomMobSpell spell,
                               CustomMobSpellScriptEngine.SpellScript script) {
        runAction(instance, caster, target, spell, script.actions(), 0);
    }

    private void runAction(CustomMobInstance instance,
                           Mob caster,
                           Player target,
                           CustomMobDefinition.CustomMobSpell spell,
                           List<CustomMobSpellScriptEngine.SpellAction> actions,
                           int index) {
        if (index >= actions.size() || caster.isDead() || target.isDead()) {
            return;
        }
        CustomMobSpellScriptEngine.SpellAction action = actions.get(index);
        if ("delay".equals(action.type())) {
            long ticks = Math.max(1L, asLong(action.args().get("ticks"), 1L));
            Bukkit.getScheduler().runTaskLater(plugin, () -> runAction(instance, caster, target, spell, actions, index + 1), ticks);
            return;
        }
        handleAction(instance, caster, target, spell, action);
        runAction(instance, caster, target, spell, actions, index + 1);
    }

    private void handleAction(CustomMobInstance instance,
                              Mob caster,
                              Player target,
                              CustomMobDefinition.CustomMobSpell spell,
                              CustomMobSpellScriptEngine.SpellAction action) {
        switch (action.type()) {
            case "animation" -> {
                String name = asString(action.args().get("name"), "shoot");
                if ("shoot".equalsIgnoreCase(name)) {
                    ModelEngineUtil.playBestShootAnimation(caster);
                } else {
                    ModelEngineUtil.playBestAttackAnimation(caster);
                }
            }
            case "sound" -> playSound(caster.getLocation(), action.args());
            case "projectile_arrow" -> castArrowShot(caster, target, spell);
            case "projectile_mage_fireball" -> castMageFireball(instance, caster, target, spell);
            case "particles_ring" -> spawnRing(caster.getLocation(), action.args());
            case "cone_damage" -> coneDamageAndKnockback(caster, spell, action.args());
            case "dash" -> dashForward(caster, action.args());
            case "teleport_target" -> teleportNearTarget(caster, target, action.args());
            case "delayed_explosion_target" -> delayedExplosionAtTarget(caster, target, spell, action.args());
            case "heal_allies" -> healNearbyAllies(instance, caster, action.args());
            case "damage_radius_target" -> damageRadiusTarget(caster, target, spell, action.args());
            case "random_strike_target_ring" -> randomStrikeTargetRing(caster, target, spell, action.args());
            case "spawn_model_vfx" -> spawnModelVfx(caster, target, action.args());
            default -> {
                // Unknown action: keep script runtime resilient.
            }
        }
    }

    private void castLegacyFallback(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        if (SPELL_MAGE_FIREBALL_BASIC.equals(spell.id())) {
            castMageFireball(instance, caster, target, spell);
            return;
        }
        if (SPELL_RANGED_ARROW_BASIC.equals(spell.id())) {
            castArrowShot(caster, target, spell);
        }
    }

    private void playSound(Location location, Map<String, Object> args) {
        String soundName = asString(args.get("id"), "ENTITY_BLAZE_SHOOT");
        float volume = (float) asDouble(args.get("volume"), 1.0);
        float pitch = (float) asDouble(args.get("pitch"), 1.0);
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void spawnRing(Location center, Map<String, Object> args) {
        double radius = Math.max(0.5, asDouble(args.get("radius"), 2.0));
        int points = Math.max(8, (int) asLong(args.get("points"), 20));
        Particle particle = parseParticle(asString(args.get("particle"), "CRIT"));
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(particle, point, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private Particle parseParticle(String id) {
        try {
            return Particle.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Particle.CRIT;
        }
    }

    private void coneDamageAndKnockback(Mob caster, CustomMobDefinition.CustomMobSpell spell, Map<String, Object> args) {
        double range = Math.max(1.0, asDouble(args.get("range"), spell.maxRange()));
        double halfAngleDeg = Math.max(5.0, asDouble(args.get("half-angle"), 30.0));
        double knockback = Math.max(0.0, asDouble(args.get("knockback"), 0.35));

        Vector forward = caster.getLocation().getDirection().normalize();
        List<LivingEntity> victims = caster.getNearbyEntities(range, 2.0, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> !entity.isDead() && !entity.equals(caster))
                .filter(entity -> isInCone(caster.getLocation(), forward, entity.getLocation(), halfAngleDeg, range))
                .collect(Collectors.toList());

        for (LivingEntity victim : victims) {
            victim.damage(spell.damage(), caster);
            Vector kb = victim.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize().multiply(knockback);
            kb.setY(Math.max(0.2, kb.getY() + 0.2));
            victim.setVelocity(victim.getVelocity().add(kb));
        }
    }

    private boolean isInCone(Location origin, Vector forward, Location target, double halfAngleDeg, double range) {
        Vector offset = target.toVector().subtract(origin.toVector());
        if (offset.lengthSquared() > range * range || offset.lengthSquared() <= 0.0001) {
            return false;
        }
        Vector direction = offset.normalize();
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, forward.dot(direction)))));
        return angle <= halfAngleDeg;
    }

    private void dashForward(Mob caster, Map<String, Object> args) {
        double strength = Math.max(0.1, asDouble(args.get("strength"), 1.0));
        Vector velocity = caster.getLocation().getDirection().normalize().multiply(strength);
        velocity.setY(Math.max(0.05, velocity.getY()));
        caster.setVelocity(velocity);
    }

    private void teleportNearTarget(Mob caster, Player target, Map<String, Object> args) {
        double behind = Math.max(0.0, asDouble(args.get("behind-distance"), 1.5));
        Vector back = target.getLocation().getDirection().normalize().multiply(-behind);
        Location to = target.getLocation().clone().add(back).add(0, 0.1, 0);
        caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 1.0, 0), 12, 0.4, 0.6, 0.4, 0.02);
        caster.teleport(to);
        caster.getWorld().spawnParticle(Particle.SMOKE, caster.getLocation().add(0, 1.0, 0), 16, 0.3, 0.6, 0.3, 0.02);
    }

    private void delayedExplosionAtTarget(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell, Map<String, Object> args) {
        long delay = Math.max(1L, asLong(args.get("delay-ticks"), 20));
        double radius = Math.max(0.5, asDouble(args.get("radius"), 2.0));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (caster.isDead() || target.isDead()) {
                return;
            }
            Location impact = target.getLocation().clone();
            impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
            impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.0f);
            for (Entity entity : impact.getWorld().getNearbyEntities(impact, radius, radius, radius)) {
                if (entity instanceof LivingEntity living && !living.equals(caster) && !living.isDead()) {
                    living.damage(spell.damage(), caster);
                }
            }
        }, delay);
    }

    private void damageRadiusTarget(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell, Map<String, Object> args) {
        double radius = Math.max(0.25, asDouble(args.get("radius"), 2.0));
        double damage = Math.max(0.0, asDouble(args.get("damage"), spell.damage()));
        int igniteTicks = Math.max(0, (int) asLong(args.get("ignite-ticks"), 0));
        double knockback = Math.max(0.0, asDouble(args.get("knockback"), 0.0));
        Location center = target.getLocation();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || living.equals(caster)) {
                continue;
            }
            living.damage(damage, caster);
            if (igniteTicks > 0) {
                living.setFireTicks(Math.max(living.getFireTicks(), igniteTicks));
            }
            if (knockback > 0.0) {
                Vector kb = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(knockback);
                kb.setY(Math.max(0.2, kb.getY() + 0.2));
                living.setVelocity(living.getVelocity().add(kb));
            }
        }
    }

    private void randomStrikeTargetRing(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell, Map<String, Object> args) {
        int strikes = Math.max(1, (int) asLong(args.get("count"), 3));
        long intervalTicks = Math.max(1L, asLong(args.get("interval-ticks"), 2));
        double minRadius = Math.max(0.0, asDouble(args.get("min-radius"), 2.0));
        double maxRadius = Math.max(minRadius, asDouble(args.get("max-radius"), 8.0));
        double hitRadius = Math.max(0.3, asDouble(args.get("hit-radius"), 2.0));
        double damage = Math.max(0.0, asDouble(args.get("damage"), spell.damage()));
        int igniteTicks = Math.max(0, (int) asLong(args.get("ignite-ticks"), 0));

        for (int i = 0; i < strikes; i++) {
            long delay = i * intervalTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (caster.isDead() || target.isDead()) {
                    return;
                }
                Location base = target.getLocation().clone();
                double angle = random.nextDouble() * (Math.PI * 2.0);
                double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
                Location strike = base.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                strike.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, strike.clone().add(0, 0.25, 0), 20, 0.35, 0.2, 0.35, 0.01);
                strike.getWorld().playSound(strike, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 1.0f);
                for (Entity entity : strike.getWorld().getNearbyEntities(strike, hitRadius, hitRadius, hitRadius)) {
                    if (entity instanceof LivingEntity living && !living.isDead() && !living.equals(caster)) {
                        living.damage(damage, caster);
                        if (igniteTicks > 0) {
                            living.setFireTicks(Math.max(living.getFireTicks(), igniteTicks));
                        }
                    }
                }
            }, delay);
        }
    }

    private void spawnModelVfx(Mob caster, Player target, Map<String, Object> args) {
        String at = asString(args.get("at"), "target");
        Location base = "caster".equalsIgnoreCase(at) ? caster.getLocation().clone() : target.getLocation().clone();
        double yOffset = asDouble(args.get("y-offset"), 0.0);
        Location spawn = base.add(0.0, yOffset, 0.0);
        long ttlTicks = Math.max(1L, asLong(args.get("ttl-ticks"), 30));
        List<String> models = asStringList(args.get("models"));
        if (models.isEmpty()) {
            String model = asString(args.get("model"), "");
            if (!model.isBlank()) {
                models = List.of(model);
            }
        }
        if (models.isEmpty()) {
            return;
        }
        ArmorStand stand = spawn.getWorld().spawn(spawn, ArmorStand.class, entity -> {
            entity.setInvisible(true);
            entity.setMarker(false);
            entity.setSmall(true);
            entity.setGravity(false);
            entity.setSilent(true);
            entity.setCollidable(false);
            entity.setInvulnerable(true);
        });
        ModelEngineUtil.applyFirstAvailableModel(stand, models, plugin);
        Bukkit.getScheduler().runTaskLater(plugin, stand::remove, ttlTicks);
    }

    private void healNearbyAllies(CustomMobInstance sourceInstance, Mob caster, Map<String, Object> args) {
        double radius = Math.max(1.0, asDouble(args.get("radius"), 10.0));
        double amount = Math.max(0.5, asDouble(args.get("amount"), 6.0));
        for (CustomMobInstance ally : mobManager.getActiveMobs().values()) {
            if (ally.entity().isDead()) {
                continue;
            }
            if (!ally.id().equalsIgnoreCase(sourceInstance.id())) {
                continue;
            }
            if (ally.entity().getLocation().getWorld() != caster.getWorld()) {
                continue;
            }
            if (ally.entity().getLocation().distanceSquared(caster.getLocation()) > radius * radius) {
                continue;
            }
            LivingEntity entity = ally.entity();
            double max = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                    ? entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                    : entity.getMaxHealth();
            entity.setHealth(Math.min(max, entity.getHealth() + amount));
            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1.1, 0), 2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    private void castArrowShot(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        Location eye = caster.getEyeLocation().clone();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        ModelEngineUtil.playBestShootAnimation(caster);
        Arrow arrow = caster.launchProjectile(Arrow.class);
        arrow.setVelocity(direction.normalize().multiply(Math.max(0.8, spell.speed())));
        arrow.setDamage(Math.max(0.1, spell.damage()));
        arrow.setShooter(caster);
        if (spell.burnTicks() > 0) {
            arrow.setFireTicks(spell.burnTicks());
        }
    }

    private void castMageFireball(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        Location eye = caster.getEyeLocation().clone();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        ModelEngineUtil.playBestShootAnimation(caster);
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
        for (Entity entity : center.getWorld().getNearbyEntities(center, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
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

    private String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private long asLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(token -> !token.isBlank())
                    .toList();
        }
        return List.of();
    }
}

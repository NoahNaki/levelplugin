package me.nakilex.levelplugin.mob.custom.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import me.nakilex.levelplugin.mob.custom.CustomMobInstance;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable runtime for custom mob spells declared in custom mob YAML definitions.
 */
public class CustomMobSpellController {
    private static final double HIT_RADIUS = 0.45;
    private static final String VFX_CURSED_FLAMES = "cursed_flames_vfx";
    private static final String VFX_CURSED_RAY = "cursed_ray_vfx";
    private static final String VFX_CURSED_ARROW_RAIN = "cursed_arrow_rain_vfx";
    private static final String VFX_CURSED_ARROW = "cursed_arrow_vfx";
    private static final String VFX_CURSED_CAST = "cursed_cast_vfx";
    private static final long VFX_LIFETIME_TICKS = 30L;

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final CustomMobSpellScriptEngine spellScriptEngine;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> globalCooldowns = new HashMap<>();

    public CustomMobSpellController(Main plugin, CustomMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.spellScriptEngine = new CustomMobSpellScriptEngine(plugin);
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
            applyPreferredDistance(instance, mob, target);
            if (isOnGlobalCooldown(mob.getUniqueId(), now)) {
                continue;
            }
            Map<String, List<CustomMobDefinition.CustomMobSpell>> eligibleByGroup =
                    collectEligibleSpells(instance, mob, target, now);
            for (List<CustomMobDefinition.CustomMobSpell> groupedSpells : eligibleByGroup.values()) {
                if (groupedSpells.isEmpty()) {
                    continue;
                }
                CustomMobDefinition.CustomMobSpell selected = selectSpell(groupedSpells);
                if (selected == null) {
                    continue;
                }
                castSpell(instance, mob, target, selected);
                markCast(instance.entity().getUniqueId(), selected, now);
                applyGlobalCooldown(instance.entity().getUniqueId(), selected, now);
            }
        }
    }

    private Map<String, List<CustomMobDefinition.CustomMobSpell>> collectEligibleSpells(CustomMobInstance instance,
                                                                                         Mob mob,
                                                                                         Player target,
                                                                                         long now) {
        Map<String, List<CustomMobDefinition.CustomMobSpell>> byGroup = new LinkedHashMap<>();
        for (CustomMobDefinition.CustomMobSpell spell : instance.definition().spells()) {
            if (!isReady(instance.entity().getUniqueId(), spell, now)) {
                continue;
            }
            if (!isInRange(mob, target, spell)) {
                continue;
            }
            if (spell.requireLineOfSight() && !mob.hasLineOfSight(target)) {
                continue;
            }
            String key = spell.selectionGroup() == null || spell.selectionGroup().isBlank()
                    ? "__" + spell.id()
                    : spell.selectionGroup();
            byGroup.computeIfAbsent(key, ignored -> new ArrayList<>()).add(spell);
        }
        return byGroup;
    }

    private CustomMobDefinition.CustomMobSpell selectSpell(List<CustomMobDefinition.CustomMobSpell> spells) {
        if (spells == null || spells.isEmpty()) {
            return null;
        }
        if (spells.size() == 1) {
            return spells.getFirst();
        }
        Map<CustomMobDefinition.CustomMobSpell, Double> weights = new LinkedHashMap<>();
        for (CustomMobDefinition.CustomMobSpell spell : spells) {
            weights.put(spell, Math.max(0.01, spell.selectionWeight()));
        }
        return RandomUtil.pickWeighted(ThreadLocalRandom.current(), weights);
    }

    private boolean isInRange(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        double distance = caster.getLocation().distance(target.getLocation());
        return distance >= Math.max(0.0, spell.minRange()) && distance <= Math.max(spell.minRange(), spell.maxRange());
    }

    private boolean isOnGlobalCooldown(UUID mobId, long now) {
        return now < globalCooldowns.getOrDefault(mobId, 0L);
    }

    private void applyPreferredDistance(CustomMobInstance instance, Mob mob, Player target) {
        if (instance == null || mob == null || target == null) {
            return;
        }
        List<CustomMobDefinition.CustomMobSpell> offensiveSpells = instance.definition().spells().stream()
                .filter(spell -> spell != null
                        && spell.damage() > 0.0
                        && Math.max(spell.minRange(), spell.maxRange()) >= 0.1)
                .toList();
        if (offensiveSpells.isEmpty()) {
            return;
        }
        List<CustomMobDefinition.CustomMobSpell> rangedSpells = offensiveSpells.stream()
                .filter(spell -> Math.max(spell.minRange(), spell.maxRange()) >= 8.0)
                .toList();
        if (rangedSpells.isEmpty()) {
            return;
        }
        long closeRangeSpellCount = offensiveSpells.stream()
                .filter(spell -> Math.max(spell.minRange(), spell.maxRange()) <= 6.0)
                .count();
        if (closeRangeSpellCount >= rangedSpells.size()) {
            return;
        }
        double desiredDistance = rangedSpells.stream()
                .mapToDouble(spell -> Math.max(spell.minRange(), spell.maxRange()) * 0.55)
                .average()
                .orElse(8.0);
        desiredDistance = Math.max(6.0, Math.min(13.0, desiredDistance));
        double distance = mob.getLocation().distance(target.getLocation());
        if (distance >= desiredDistance - 0.75) {
            return;
        }
        Vector retreat = mob.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.0);
        if (retreat.lengthSquared() <= 0.0001) {
            return;
        }
        retreat.normalize().multiply(0.26);
        mob.setVelocity(mob.getVelocity().multiply(0.45).add(retreat).setY(Math.max(-0.08, mob.getVelocity().getY())));
        faceTarget(mob, target);
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
        globalCooldowns.remove(mobId);
    }

    public boolean debugCastSpell(LivingEntity casterEntity, Player target, String spellId) {
        if (!(casterEntity instanceof Mob caster) || target == null || spellId == null || spellId.isBlank()) {
            return false;
        }
        var instanceOpt = mobManager.getInstance(casterEntity);
        if (instanceOpt.isEmpty()) {
            return false;
        }
        CustomMobInstance instance = instanceOpt.get();
        CustomMobDefinition.CustomMobSpell spell = instance.definition().spells().stream()
                .filter(Objects::nonNull)
                .filter(defSpell -> spellId.equalsIgnoreCase(defSpell.id())
                        || (defSpell.scriptKey() != null && spellId.equalsIgnoreCase(defSpell.scriptKey())))
                .findFirst()
                .orElse(null);
        if (spell == null) {
            return false;
        }
        castSpell(instance, caster, target, spell);
        return true;
    }

    private void applyGlobalCooldown(UUID mobId, CustomMobDefinition.CustomMobSpell spell, long now) {
        if (spell.gcdTicks() <= 0) {
            return;
        }
        globalCooldowns.put(mobId, now + (spell.gcdTicks() * 50L));
    }

    private void castSpell(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        CustomMobSpellScriptEngine.SpellExecutionContext scriptContext =
                new CustomMobSpellScriptEngine.SpellExecutionContext(caster, target, instance, spell);
        String scriptId = spell.scriptKey() != null && !spell.scriptKey().isBlank()
                ? spell.scriptKey()
                : spell.id();
        if (spellScriptEngine.execute(scriptId, scriptContext, this::handleScriptAction)) {
            return;
        }
        if ("mage_fireball_basic".equals(spell.id())) {
            castMageFireball(instance, caster, target, spell, true);
            return;
        }
        if ("ranged_arrow_basic".equals(spell.id())) {
            castArrowShot(caster, target, spell, true);
        }
    }

    private void handleScriptAction(CustomMobSpellScriptEngine.SpellActionDef action,
                                    CustomMobSpellScriptEngine.SpellExecutionContext context) {
        if (action == null || context == null || context.caster() == null || context.target() == null) {
            return;
        }
        Mob caster = context.caster();
        Player target = context.target();
        CustomMobDefinition.CustomMobSpell spell = context.spell();
        switch (action.type()) {
            case "play-animation" -> {
                String animation = action.params().getString("animation", "");
                if (playNamedAnimation(caster, animation)) {
                    emitSpellAnimationDebug(target, caster, spell.id(), animation);
                }
            }
            case "face-target" -> faceTarget(caster, target);
            case "archer-shoot-sound" -> playArcherShootSounds(caster.getLocation());
            case "archer-special-sound" -> playArcherSpecialSounds(caster.getLocation());
            case "shoot-arrow" -> castArrowShot(caster, target, spell, false);
            case "play-sound" -> {
                String soundToken = action.params().getString("sound", "");
                Sound sound = parseSound(soundToken);
                if (sound != null && caster.getWorld() != null) {
                    float volume = (float) action.params().getDouble("volume", 1.0);
                    float pitch = (float) action.params().getDouble("pitch", 1.0);
                    caster.getWorld().playSound(caster.getLocation(), sound, volume, pitch);
                }
            }
            case "cast-mage-fireball" -> {
                if (context.instance() != null) {
                    castMageFireball(context.instance(), caster, target, spell, false);
                }
            }
            case "mage-burst" -> {
                String animation = action.params().getString("animation", "attack_1");
                long windup = Math.max(0L, action.params().getLong("windup-ticks", 20L));
                int projectiles = Math.max(1, action.params().getInt("projectiles", 1));
                long intervalTicks = Math.max(1L, action.params().getLong("interval-ticks", 15L));
                String model = action.params().getString("model", VFX_CURSED_CAST);
                double damageMultiplier = Math.max(0.1, action.params().getDouble("damage-multiplier", 1.0));
                double speedMultiplier = Math.max(0.1, action.params().getDouble("speed-multiplier", 1.0));
                double speedMin = Math.max(0.1, action.params().getDouble("speed-min", 0.8));
                castCursedMageBurst(caster, target, spell, animation, windup, projectiles, intervalTicks, model,
                        Math.max(0.1, spell.damage() * damageMultiplier),
                        Math.max(0, spell.burnTicks()),
                        Math.max(speedMin, spell.speed() * speedMultiplier));
            }
            case "arrow-rain" -> {
                int count = Math.max(1, action.params().getInt("count", 8));
                long interval = Math.max(1L, action.params().getLong("interval-ticks", 2L));
                double multiplier = Math.max(0.1, action.params().getDouble("hit-damage-multiplier", 0.55));
                startArrowRainVolley(caster, target, count, interval, Math.max(0.1, spell.damage() * multiplier));
            }
            case "launch-model-projectile" -> {
                String model = action.params().getString("model", VFX_CURSED_ARROW);
                double minSpeed = Math.max(0.1, action.params().getDouble("speed-min", 1.5));
                boolean useImpactFx = action.params().getBoolean("use-impact-fx", true);
                launchModelProjectile(caster, target, model, Math.max(minSpeed, spell.speed()),
                        Math.max(0.1, spell.damage()), spell.burnTicks(), useImpactFx);
            }
            case "set-caster-horizontal-velocity" -> {
                double horizontal = action.params().getDouble("speed", 0.0);
                caster.setVelocity(new Vector(horizontal, caster.getVelocity().getY(), horizontal));
            }
            case "deal-cone-damage" -> dealConeDamageToPlayers(
                    caster,
                    Math.max(0.1, spell.damage() * Math.max(0.0, action.params().getDouble("damage-multiplier", 1.0))),
                    Math.max(0.5, action.params().getDouble("range", 3.0)),
                    Math.max(1.0, action.params().getDouble("half-angle-degrees", 90.0)),
                    Math.max(0.0, action.params().getDouble("knockback-horizontal", 0.0)),
                    Math.max(0.0, action.params().getDouble("knockback-vertical", 0.0))
            );
            case "throw-players-from-caster" -> throwPlayersFrom(
                    caster.getLocation(),
                    Math.max(0.5, action.params().getDouble("radius", 3.0)),
                    Math.max(0.0, action.params().getDouble("horizontal-strength", 1.0)),
                    Math.max(0.0, action.params().getDouble("vertical-strength", 0.25))
            );
            case "spawn-ring-vfx" -> spawnRingVfx(
                    caster.getLocation(),
                    action.params().getString("model", VFX_CURSED_FLAMES),
                    Math.max(1, action.params().getInt("points", 12)),
                    Math.max(0.1, action.params().getDouble("radius", 4.0))
            );
            case "schedule-area-pulses" -> {
                String token = action.params().getString("damages", "");
                List<Double> damages = parseDamageList(token, Math.max(0.1, spell.damage()));
                scheduleAreaPulseSeries(
                        caster,
                        caster.getLocation().clone().add(0.0, action.params().getDouble("y-offset", 0.1), 0.0),
                        damages,
                        Math.max(1L, action.params().getLong("interval-ticks", 10L)),
                        Math.max(0.5, action.params().getDouble("radius", 2.0)),
                        action.params().getBoolean("ignite", false),
                        Math.max(0, action.params().getInt("ignite-ticks", 0)),
                        null
                );
            }
            case "ray-burst-series" -> {
                int bursts = Math.max(1, action.params().getInt("bursts", 5));
                long interval = Math.max(1L, action.params().getLong("interval-ticks", 2L));
                double minRadius = Math.max(0.1, action.params().getDouble("min-radius", 4.0));
                double maxRadius = Math.max(minRadius, action.params().getDouble("max-radius", 10.0));
                String model = action.params().getString("model", VFX_CURSED_RAY);
                long pulseDelay = Math.max(0L, action.params().getLong("pulse-delay-ticks", 12L));
                String pulseToken = action.params().getString("pulse-damages", "1.0,1.0,1.0,1.0,1.0");
                List<Double> pulses = parseDamageList(pulseToken, Math.max(0.1, spell.damage()));
                for (int i = 0; i < bursts; i++) {
                    runLater(i * interval, () -> {
                        if (caster.isDead() || caster.getWorld() == null) {
                            return;
                        }
                        Location strike = randomRingLocation(caster.getLocation(), minRadius, maxRadius);
                        spawnTemporaryModelVfx(strike, model, VFX_LIFETIME_TICKS);
                        runLater(pulseDelay, () -> scheduleAreaPulseSeries(caster, strike, pulses,
                                Math.max(1L, action.params().getLong("pulse-interval-ticks", 10L)),
                                Math.max(0.5, action.params().getDouble("pulse-radius", 2.0)),
                                action.params().getBoolean("ignite", true),
                                Math.max(0, action.params().getInt("ignite-ticks", 100)),
                                () -> playRayHum(strike)));
                    });
                }
            }
            case "dash-toward-target" -> dashTowardTarget(caster, target, Math.max(0.05, action.params().getDouble("speed", 0.5)));
            case "start-rush" -> startRush(
                    caster,
                    target,
                    Math.max(1, action.params().getInt("duration-ticks", 20)),
                    Math.max(0.05, action.params().getDouble("dash-per-tick", 0.3)),
                    Math.max(0.5, action.params().getDouble("hit-radius", 2.0)),
                    Math.max(0.1, spell.damage() * Math.max(0.0, action.params().getDouble("damage-multiplier", 1.0)))
            );
            case "spawn-ground-impact" -> spawnGroundImpact(caster.getLocation());
            case "teleport-near-target" -> {
                Location to = target.getLocation().clone()
                        .add(randomOffset(Math.max(0.1, action.params().getDouble("offset-xz", 1.5))),
                                action.params().getDouble("offset-y", 0.0),
                                randomOffset(Math.max(0.1, action.params().getDouble("offset-xz", 1.5))));
                caster.teleport(to);
            }
            case "spawn-smoke-at-caster" -> spawnSmoke(caster.getLocation(), Math.max(1, action.params().getInt("amount", 20)));
            case "explosion-at-target" -> {
                Location impact = target.getLocation().clone();
                if (impact.getWorld() != null) {
                    impact.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, impact.clone().add(0.0, 0.2, 0.0), 1);
                    impact.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, impact.clone().add(0.0, 0.2, 0.0), 20, 0.45, 0.2, 0.45, 0.02);
                    impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
                    for (Player player : getNearbyPlayers(impact, Math.max(0.5, action.params().getDouble("radius", 5.0)))) {
                        player.damage(Math.max(0.1, spell.damage() * Math.max(0.0, action.params().getDouble("damage-multiplier", 1.5))), caster);
                    }
                }
            }
            case "heal-nearby-allied-custom-mobs" -> {
                String prefix = action.params().getString("id-prefix", "goblin_").toLowerCase(Locale.ROOT);
                double healAmount = Math.max(0.1, action.params().getDouble("heal-amount", spell.damage()));
                for (LivingEntity ally : getNearbyAlliedCustomMobs(
                        caster,
                        Math.max(0.5, action.params().getDouble("radius", 20.0)),
                        def -> def.id().toLowerCase(Locale.ROOT).startsWith(prefix),
                        Math.max(1, action.params().getInt("limit", 4)))) {
                    var maxHealthAttribute = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
                    double maxHealth = maxHealthAttribute != null && ally.getAttribute(maxHealthAttribute) != null
                            ? ally.getAttribute(maxHealthAttribute).getValue()
                            : ally.getHealth();
                    ally.setHealth(Math.min(maxHealth, ally.getHealth() + healAmount));
                    ally.getWorld().spawnParticle(org.bukkit.Particle.DUST_COLOR_TRANSITION, ally.getLocation().add(0.0, 0.9, 0.0),
                            16, 0.3, 0.6, 0.3, 0.0,
                            new org.bukkit.Particle.DustTransition(org.bukkit.Color.fromRGB(0xE9FF26), org.bukkit.Color.fromRGB(0x2BFF99), 0.8f));
                }
            }
            default -> {
            }
        }
    }

    private List<Double> parseDamageList(String token, double spellDamageBase) {
        if (token == null || token.isBlank()) {
            return List.of(spellDamageBase);
        }
        List<Double> values = new ArrayList<>();
        for (String part : token.split(",")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                values.add(Math.max(0.0, Double.parseDouble(part.trim()) * spellDamageBase));
            } catch (NumberFormatException ignored) {
            }
        }
        return values.isEmpty() ? List.of(spellDamageBase) : values;
    }

    private Sound parseSound(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(token.trim().toUpperCase(Locale.ROOT).replace('.', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void castArrowShot(Mob caster,
                               Player target,
                               CustomMobDefinition.CustomMobSpell spell,
                               boolean triggerAnimation) {
        Location eye = caster.getEyeLocation().clone();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        if (triggerAnimation) {
            ModelEngineUtil.triggerActionState(caster, List.of("shoot", "arrow", "bow", "cast", "attack"), 500L);
            emitSpellAnimationDebug(target, caster, spell.id(), inferAnimationForSpell(spell.id(), "shoot"));
        }
        Arrow arrow = caster.launchProjectile(Arrow.class);
        arrow.setVelocity(direction.normalize().multiply(Math.max(0.8, spell.speed())));
        arrow.setDamage(Math.max(0.1, spell.damage()));
        arrow.setShooter(caster);
        if (spell.burnTicks() > 0) {
            arrow.setFireTicks(spell.burnTicks());
        }
    }

    private void castMageFireball(CustomMobInstance instance,
                                  Mob caster,
                                  Player target,
                                  CustomMobDefinition.CustomMobSpell spell,
                                  boolean triggerAnimation) {
        Location eye = caster.getEyeLocation().clone();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        if (triggerAnimation) {
            ModelEngineUtil.triggerActionState(caster, List.of("shoot", "arrow", "bow", "cast", "attack"), 600L);
            emitSpellAnimationDebug(target, caster, spell.id(), inferAnimationForSpell(spell.id(), "shoot"));
        }
        MageFireballBasicAttackSpell.FireballSpawnResult spawnResult =
                MageFireballBasicAttackSpell.spawnProjectileAnchor(plugin, eye, direction);
        if (spawnResult == null) {
            return;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.65f, 0.9f);
        launchProjectile(instance, caster, target, spawnResult.anchor(), direction.normalize(), spell);
    }

    private void castCursedMageBurst(Mob caster,
                                     Player target,
                                     CustomMobDefinition.CustomMobSpell spell,
                                     String animation,
                                     long windupTicks,
                                     int projectiles,
                                     long projectileIntervalTicks,
                                     String vfxModelId,
                                     double damage,
                                     int burnTicks,
                                     double speed) {
        if (!isCombatContextValid(caster, target)) {
            return;
        }
        playNamedAnimation(caster, animation);
        faceTarget(caster, target);
        runLater(windupTicks, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            for (int i = 0; i < Math.max(1, projectiles); i++) {
                long delay = i * Math.max(1L, projectileIntervalTicks);
                runLater(delay, () -> {
                    if (!isCombatContextValid(caster, target)) {
                        return;
                    }
                    faceTarget(caster, target);
                    playMageCastSounds(caster.getLocation());
                    launchModelProjectile(caster, target, vfxModelId, speed, damage, burnTicks, true);
                });
            }
        });
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

    private boolean playNamedAnimation(Mob caster, String animationName) {
        boolean played = ModelEngineUtil.playAnimationByName(caster, animationName, false);
        if (played) {
            ModelEngineUtil.holdActionState(caster, 600L);
            return true;
        }
        if (animationName != null && !animationName.isBlank()) {
            return ModelEngineUtil.triggerActionState(caster,
                    List.of(animationName, "shoot", "attack", "cast", "slash", "swing"),
                    600L);
        }
        return false;
    }

    private String inferAnimationForSpell(String spellId, String fallback) {
        if (spellId == null || spellId.isBlank()) {
            return fallback;
        }
        String lower = spellId.toLowerCase(Locale.ROOT);
        if (!lower.contains("shoot")) {
            return fallback;
        }
        int idx = lower.lastIndexOf('_');
        if (idx > 0 && idx < lower.length() - 1) {
            String suffix = lower.substring(idx + 1);
            if (suffix.chars().allMatch(Character::isDigit)) {
                return "shoot_" + suffix;
            }
        }
        return "shoot";
    }

    private void emitSpellAnimationDebug(Player target, Mob caster, String spellId, String animationName) {
        if (target == null || caster == null || !target.isOnline() || target.isDead()) {
            return;
        }
        String safeSpell = (spellId == null || spellId.isBlank()) ? "unknown_spell" : spellId;
        String safeAnimation = (animationName == null || animationName.isBlank()) ? "unknown_animation" : animationName;
        String message = ChatColor.GRAY + "[MobCast] "
                + ChatColor.WHITE + caster.getType().name()
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.AQUA + "spell=" + safeSpell
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.LIGHT_PURPLE + "animation=" + safeAnimation;
        ChatMessageUtil.send(target, ChatMessageUtil.MessageType.INFO, message);
    }

    private boolean isCombatContextValid(Mob caster, Player target) {
        return caster != null
                && target != null
                && caster.isValid()
                && !caster.isDead()
                && target.isOnline()
                && !target.isDead()
                && caster.getWorld() != null
                && caster.getWorld().equals(target.getWorld());
    }

    private void runLater(long ticks, Runnable runnable) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, Math.max(0L, ticks));
    }

    private void faceTarget(Mob caster, Player target) {
        if (!isCombatContextValid(caster, target)) {
            return;
        }
        Location from = caster.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(from.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        ModelEngineUtil.orientEntityToVector(caster, direction);
    }

    private void playArcherShootSounds(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        at.getWorld().playSound(at, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
        at.getWorld().playSound(at, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_SWIM, 0.4f, 0.9f);
    }

    private void playArcherSpecialSounds(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 0.5f);
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 0.9f);
        at.getWorld().playSound(at, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.6f, 1.0f);
        at.getWorld().playSound(at, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.7f, 1.2f);
        at.getWorld().playSound(at, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.8f);
        at.getWorld().playSound(at, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.1f);
        at.getWorld().playSound(at, Sound.ENTITY_GHAST_SHOOT, 0.7f, 0.6f);
        at.getWorld().playSound(at, Sound.ITEM_FIRECHARGE_USE, 1.1f, 0.9f);
    }

    private void playMageCastSounds(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 0.5f);
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 0.9f);
        at.getWorld().playSound(at, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.6f, 1.0f);
        at.getWorld().playSound(at, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.7f, 1.2f);
    }

    private void playRayHum(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        at.getWorld().playSound(at, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 1.0f);
        at.getWorld().playSound(at, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 2.0f, 0.8f);
        at.getWorld().playSound(at, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 2.0f, 0.9f);
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 0.5f);
    }

    private void scheduleAreaPulseSeries(Mob caster,
                                         Location center,
                                         List<Double> damages,
                                         long intervalTicks,
                                         double radius,
                                         boolean ignite,
                                         int igniteTicks,
                                         Runnable perPulseSound) {
        if (caster == null || center == null || center.getWorld() == null || damages == null || damages.isEmpty()) {
            return;
        }
        for (int i = 0; i < damages.size(); i++) {
            double pulseDamage = Math.max(0.0, damages.get(i));
            long delay = i * Math.max(1L, intervalTicks);
            runLater(delay, () -> {
                if (caster.isDead() || center.getWorld() == null) {
                    return;
                }
                if (perPulseSound != null) {
                    perPulseSound.run();
                }
                for (Player player : getNearbyPlayers(center, radius)) {
                    if (pulseDamage > 0.0) {
                        player.damage(Math.max(0.1, pulseDamage), caster);
                    }
                    if (ignite && igniteTicks > 0) {
                        player.setFireTicks(Math.max(player.getFireTicks(), igniteTicks));
                    }
                }
            });
        }
    }

    private void startArrowRainVolley(Mob caster,
                                      Player anchorTarget,
                                      int arrowCount,
                                      long intervalTicks,
                                      double hitDamage) {
        if (!isCombatContextValid(caster, anchorTarget)) {
            return;
        }
        for (int i = 0; i < Math.max(1, arrowCount); i++) {
            long delay = i * Math.max(1L, intervalTicks);
            runLater(delay, () -> {
                if (!isCombatContextValid(caster, anchorTarget)) {
                    return;
                }
                Location targetPoint = anchorTarget.getLocation().clone()
                        .add(randomOffset(3.0), 0.2, randomOffset(3.0));
                launchFallingStrike(caster, targetPoint, VFX_CURSED_ARROW_RAIN, 8, hitDamage, 1.45);
            });
        }
    }

    private void launchFallingStrike(Mob caster,
                                     Location groundImpact,
                                     String modelId,
                                     int travelTicks,
                                     double hitDamage,
                                     double radius) {
        if (caster == null || caster.isDead() || groundImpact == null || groundImpact.getWorld() == null) {
            return;
        }
        Location start = groundImpact.clone().add(randomOffset(0.5), 8.0 + ThreadLocalRandom.current().nextDouble(1.5), randomOffset(0.5));
        ArmorStand strike = spawnModelVfxAnchor(start, modelId);
        if (strike == null) {
            return;
        }
        int ticks = Math.max(1, travelTicks);
        Vector step = groundImpact.toVector().subtract(start.toVector()).multiply(1.0 / ticks);
        new BukkitRunnable() {
            int lived;
            @Override
            public void run() {
                if (!strike.isValid() || caster.isDead()) {
                    removeProjectile(strike);
                    cancel();
                    return;
                }
                if (lived >= ticks) {
                    Location impact = strike.getLocation().clone();
                    impact.getWorld().spawnParticle(org.bukkit.Particle.CRIT, impact, 8, 0.25, 0.15, 0.25, 0.02);
                    scheduleAreaPulseSeries(caster, impact,
                            List.of(hitDamage, hitDamage * 2.0, hitDamage, hitDamage, hitDamage),
                            2L, Math.max(0.5, radius), false, 0,
                            () -> playArcherShootSounds(impact));
                    removeProjectile(strike);
                    cancel();
                    return;
                }
                strike.teleport(strike.getLocation().add(step));
                lived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void launchModelProjectile(Mob caster,
                                       LivingEntity intendedTarget,
                                       String modelId,
                                       double speed,
                                       double damage,
                                       int burnTicks,
                                       boolean useImpactFx) {
        if (caster == null || caster.getWorld() == null || intendedTarget == null || intendedTarget.isDead()) {
            return;
        }
        Location origin = caster.getEyeLocation().clone().add(0.0, -0.1, 0.0);
        Vector direction = intendedTarget.getEyeLocation().toVector().subtract(origin.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return;
        }
        Vector step = direction.normalize().multiply(Math.max(0.15, speed * 0.1));
        ArmorStand projectile = origin.getWorld().spawn(origin, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setCollidable(false);
        });
        if (modelId != null && !modelId.isBlank()) {
            ModelEngineUtil.applyFirstAvailableModel(projectile, ModelEngineUtil.buildModelCandidates(modelId), plugin);
        }
        double maxDistanceSq = 38.0 * 38.0;
        Location start = projectile.getLocation().clone();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || caster.isDead() || intendedTarget.isDead()) {
                    removeProjectile(projectile);
                    cancel();
                    return;
                }
                Location current = projectile.getLocation();
                if (current.distanceSquared(start) >= maxDistanceSq) {
                    removeProjectile(projectile);
                    cancel();
                    return;
                }
                Location next = current.clone().add(step);
                projectile.teleport(next);
                ModelEngineUtil.orientEntityToVector(projectile, step);
                LivingEntity hit = findTargetAt(next, caster, projectile);
                if (hit == null) {
                    return;
                }
                if (useImpactFx) {
                    SpellEffectUtil.spawnFireImpactEffect(next);
                    next.getWorld().playSound(next, Sound.BLOCK_FIRE_EXTINGUISH, 0.85f, 0.75f);
                }
                hit.damage(Math.max(0.1, damage), caster);
                if (burnTicks > 0) {
                    hit.setFireTicks(Math.max(hit.getFireTicks(), burnTicks));
                }
                removeProjectile(projectile);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void dashTowardTarget(Mob caster, LivingEntity target, double speed) {
        if (caster == null || target == null) {
            return;
        }
        Vector dash = target.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0.0);
        if (dash.lengthSquared() <= 0.0001) {
            return;
        }
        caster.setVelocity(caster.getVelocity().multiply(0.45).add(dash.normalize().multiply(Math.max(0.05, speed)).setY(0.08)));
    }

    private void startRush(Mob caster,
                           LivingEntity target,
                           int durationTicks,
                           double dashPerTick,
                           double hitRadius,
                           double hitDamage) {
        new BukkitRunnable() {
            int ticks;

            @Override
            public void run() {
                if (caster == null || target == null || caster.isDead() || target.isDead() || ticks >= durationTicks) {
                    cancel();
                    return;
                }
                faceTarget(caster, (target instanceof Player player) ? player : null);
                dashTowardTarget(caster, target, dashPerTick);
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GRAVEL_STEP, 0.55f, 1.0f);
                caster.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, caster.getLocation().add(0.0, 0.1, 0.0),
                        3, 0.3, 0.1, 0.3, 0.02);
                for (Player player : getNearbyPlayers(caster.getLocation(), hitRadius)) {
                    player.damage(hitDamage, caster);
                    throwPlayersFrom(caster.getLocation(), hitRadius, 0.8, 0.25);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnGroundImpact(Location center) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        center.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0.0, 0.1, 0.0),
                5, 0.4, 0.1, 0.4, 0.02);
        center.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, center.clone().add(0.0, 0.1, 0.0),
                15, 0.6, 0.1, 0.6, org.bukkit.Material.DIRT.createBlockData());
    }

    private void spawnSmoke(Location center, int amount) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        center.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, center, Math.max(1, amount), 1.0, 0.6, 1.0, 0.1);
    }

    private double randomOffset(double range) {
        return ThreadLocalRandom.current().nextDouble(-Math.max(0.0, range), Math.max(0.0, range));
    }

    private List<LivingEntity> getNearbyAlliedCustomMobs(Mob source,
                                                         double radius,
                                                         java.util.function.Predicate<CustomMobDefinition> filter,
                                                         int limit) {
        if (source == null || source.getWorld() == null || radius <= 0.0) {
            return List.of();
        }
        List<LivingEntity> allies = new ArrayList<>();
        for (var entity : source.getWorld().getNearbyEntities(source.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || living.equals(source)) {
                continue;
            }
            var instance = mobManager.getInstance(living);
            if (instance.isEmpty()) {
                continue;
            }
            if (filter != null && !filter.test(instance.get().definition())) {
                continue;
            }
            allies.add(living);
            if (allies.size() >= Math.max(1, limit)) {
                break;
            }
        }
        return allies;
    }

    private void dealConeDamageToPlayers(Mob caster,
                                         double damage,
                                         double range,
                                         double halfAngleDegrees,
                                         double knockbackHorizontal,
                                         double knockbackVertical) {
        if (caster == null || caster.getWorld() == null || damage <= 0.0) {
            return;
        }
        Vector forward = caster.getLocation().getDirection().clone().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            return;
        }
        forward.normalize();
        double dotThreshold = Math.cos(Math.toRadians(Math.max(1.0, halfAngleDegrees)));
        for (Player player : getNearbyPlayers(caster.getLocation(), range + 0.75)) {
            Vector toPlayer = player.getLocation().toVector().subtract(caster.getLocation().toVector());
            Vector flat = toPlayer.clone().setY(0.0);
            if (flat.lengthSquared() <= 0.0001 || flat.lengthSquared() > (range * range)) {
                continue;
            }
            Vector dirToPlayer = flat.clone().normalize();
            if (forward.dot(dirToPlayer) < dotThreshold) {
                continue;
            }
            player.damage(Math.max(0.1, damage), caster);
            Vector shove = dirToPlayer.multiply(Math.max(0.0, knockbackHorizontal)).setY(Math.max(0.0, knockbackVertical));
            player.setVelocity(player.getVelocity().multiply(0.6).add(shove));
        }
    }

    private void throwPlayersFrom(Location origin, double radius, double horizontalStrength, double verticalStrength) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        for (Player player : getNearbyPlayers(origin, radius)) {
            Vector away = player.getLocation().toVector().subtract(origin.toVector()).setY(0.0);
            if (away.lengthSquared() <= 0.0001) {
                away = origin.getDirection().clone().setY(0.0);
            }
            if (away.lengthSquared() <= 0.0001) {
                continue;
            }
            Vector velocity = away.normalize().multiply(Math.max(0.0, horizontalStrength)).setY(Math.max(0.0, verticalStrength));
            player.setVelocity(player.getVelocity().multiply(0.55).add(velocity));
        }
    }

    private void spawnRingVfx(Location center, String modelId, int points, double radius) {
        if (center == null || center.getWorld() == null || points <= 0 || radius <= 0.0) {
            return;
        }
        for (int i = 0; i < points; i++) {
            double theta = (Math.PI * 2 * i) / points;
            Location point = center.clone().add(Math.cos(theta) * radius, 0.05, Math.sin(theta) * radius);
            spawnTemporaryModelVfx(point, modelId, VFX_LIFETIME_TICKS);
        }
    }

    private void spawnTemporaryModelVfx(Location location, String modelId, long lifeTicks) {
        if (location == null || location.getWorld() == null || modelId == null || modelId.isBlank()) {
            return;
        }
        ArmorStand stand = spawnModelVfxAnchor(location, modelId);
        if (stand == null) {
            return;
        }
        runLater(Math.max(1L, lifeTicks), () -> {
            if (stand.isValid()) {
                stand.remove();
            }
        });
    }

    private ArmorStand spawnModelVfxAnchor(Location location, String modelId) {
        if (location == null || location.getWorld() == null || modelId == null || modelId.isBlank()) {
            return null;
        }
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, entity -> {
            entity.setInvisible(true);
            entity.setGravity(false);
            entity.setMarker(true);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setPersistent(false);
            entity.setCollidable(false);
        });
        ModelEngineUtil.applyFirstAvailableModel(stand, ModelEngineUtil.buildModelCandidates(modelId), plugin);
        return stand;
    }

    private Location randomRingLocation(Location center, double minRadius, double maxRadius) {
        double min = Math.max(0.0, Math.min(minRadius, maxRadius));
        double max = Math.max(min, Math.max(minRadius, maxRadius));
        double radius = ThreadLocalRandom.current().nextDouble(min, max + 0.00001);
        double theta = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        return center.clone().add(Math.cos(theta) * radius, 0.1, Math.sin(theta) * radius);
    }

    private List<Player> getNearbyPlayers(Location center, double radius) {
        if (center == null || center.getWorld() == null) {
            return List.of();
        }
        double radiusSq = radius * radius;
        List<Player> players = new ArrayList<>();
        for (var entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player player) || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            players.add(player);
        }
        return players;
    }
}

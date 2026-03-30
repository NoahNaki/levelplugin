package me.nakilex.levelplugin.mob.custom.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import me.nakilex.levelplugin.mob.custom.CustomMobInstance;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.RandomUtil;
import org.bukkit.Bukkit;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reusable runtime for custom mob spells declared in custom mob YAML definitions.
 */
public class CustomMobSpellController {
    private static final String SPELL_MAGE_FIREBALL_BASIC = "mage_fireball_basic";
    private static final String SPELL_RANGED_ARROW_BASIC = "ranged_arrow_basic";
    private static final String SPELL_CURSED_KNIGHT_ATTACK_1 = "cursed_knight_attack_1";
    private static final String SPELL_CURSED_KNIGHT_ATTACK_2 = "cursed_knight_attack_2";
    private static final String SPELL_CURSED_KNIGHT_ATTACK_3 = "cursed_knight_attack_3";
    private static final String SPELL_CURSED_ARCHER_SHOOT_1 = "cursed_archer_shoot_1";
    private static final String SPELL_CURSED_ARCHER_SHOOT_2 = "cursed_archer_shoot_2";
    private static final String SPELL_CURSED_ARCHER_SHOOT_3 = "cursed_archer_shoot_3";
    private static final String SPELL_CURSED_MAGE_SPELL_1 = "cursed_mage_spell_1";
    private static final String SPELL_CURSED_MAGE_SPELL_2 = "cursed_mage_spell_2";
    private static final String SPELL_CURSED_MAGE_SPELL_3 = "cursed_mage_spell_3";
    private static final String SPELL_GOBLIN_WARRIOR_SWORD_SLAM = "goblin_warrior_sword_slam";
    private static final String SPELL_GOBLIN_WARRIOR_SHIELD_RUSH = "goblin_warrior_shield_rush";
    private static final String SPELL_GOBLIN_ASSASSIN_SHADOWSTEP = "goblin_assassin_shadowstep";
    private static final String SPELL_GOBLIN_ASSASSIN_STAB = "goblin_assassin_stab";
    private static final String SPELL_GOBLIN_ASSASSIN_SLASH = "goblin_assassin_slash";
    private static final String SPELL_GOBLIN_ARCHER_SHOOT = "goblin_archer_shoot";
    private static final String SPELL_GOBLIN_ARCHER_THROW_BOMB = "goblin_archer_throw_bomb";
    private static final String SPELL_GOBLIN_SHAMAN_FIREBALL = "goblin_shaman_fireball";
    private static final String SPELL_GOBLIN_SHAMAN_HEAL = "goblin_shaman_heal";
    private static final Set<String> RANGED_SPELL_IDS = Set.of(
            SPELL_CURSED_ARCHER_SHOOT_1,
            SPELL_CURSED_ARCHER_SHOOT_2,
            SPELL_CURSED_ARCHER_SHOOT_3,
            SPELL_CURSED_MAGE_SPELL_1,
            SPELL_CURSED_MAGE_SPELL_2,
            SPELL_CURSED_MAGE_SPELL_3,
            SPELL_GOBLIN_ARCHER_SHOOT,
            SPELL_GOBLIN_ARCHER_THROW_BOMB,
            SPELL_GOBLIN_SHAMAN_FIREBALL,
            SPELL_GOBLIN_SHAMAN_HEAL,
            SPELL_MAGE_FIREBALL_BASIC,
            SPELL_RANGED_ARROW_BASIC
    );
    private static final double HIT_RADIUS = 0.45;
    private static final String VFX_CURSED_FLAMES = "cursed_flames_vfx";
    private static final String VFX_CURSED_RAY = "cursed_ray_vfx";
    private static final String VFX_CURSED_ARROW_RAIN = "cursed_arrow_rain_vfx";
    private static final String VFX_CURSED_ARROW = "cursed_arrow_vfx";
    private static final String VFX_CURSED_CAST = "cursed_cast_vfx";
    private static final String VFX_CURSED_SLASH = "cursed_slash_vfx";
    private static final String VFX_CURSED_HOLLOW = "cursed_hollow_vfx";
    private static final long VFX_LIFETIME_TICKS = 30L;

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> globalCooldowns = new HashMap<>();

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
            applyRangedSpacing(instance, mob, target);
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

    private void applyRangedSpacing(CustomMobInstance instance, Mob mob, Player target) {
        if (instance == null || mob == null || target == null) {
            return;
        }
        boolean isRanged = instance.definition().spells().stream()
                .map(CustomMobDefinition.CustomMobSpell::id)
                .anyMatch(RANGED_SPELL_IDS::contains);
        if (!isRanged) {
            return;
        }
        double distance = mob.getLocation().distance(target.getLocation());
        double retreatDistance = 6.0;
        if (distance >= retreatDistance) {
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

    private void applyGlobalCooldown(UUID mobId, CustomMobDefinition.CustomMobSpell spell, long now) {
        if (spell.gcdTicks() <= 0) {
            return;
        }
        globalCooldowns.put(mobId, now + (spell.gcdTicks() * 50L));
    }

    private void castSpell(CustomMobInstance instance, Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        if (SPELL_MAGE_FIREBALL_BASIC.equals(spell.id())) {
            castMageFireball(instance, caster, target, spell);
            return;
        }
        if (SPELL_RANGED_ARROW_BASIC.equals(spell.id())) {
            castArrowShot(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_KNIGHT_ATTACK_1.equals(spell.id())) {
            castCursedKnightAttackOne(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_KNIGHT_ATTACK_2.equals(spell.id())) {
            castCursedKnightAttackTwo(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_KNIGHT_ATTACK_3.equals(spell.id())) {
            castCursedKnightAttackThree(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_ARCHER_SHOOT_1.equals(spell.id())) {
            castCursedArcherShootOne(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_ARCHER_SHOOT_2.equals(spell.id())) {
            castCursedArcherShootTwo(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_ARCHER_SHOOT_3.equals(spell.id())) {
            castCursedArcherShootThree(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_MAGE_SPELL_1.equals(spell.id())) {
            castCursedMageSpellOne(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_MAGE_SPELL_2.equals(spell.id())) {
            castCursedMageSpellTwo(caster, target, spell);
            return;
        }
        if (SPELL_CURSED_MAGE_SPELL_3.equals(spell.id())) {
            castCursedMageSpellThree(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_WARRIOR_SWORD_SLAM.equals(spell.id())) {
            castGoblinWarriorSwordSlam(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_WARRIOR_SHIELD_RUSH.equals(spell.id())) {
            castGoblinWarriorShieldRush(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_ASSASSIN_SHADOWSTEP.equals(spell.id())) {
            castGoblinAssassinShadowstep(caster, target);
            return;
        }
        if (SPELL_GOBLIN_ASSASSIN_STAB.equals(spell.id())) {
            castGoblinAssassinStab(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_ASSASSIN_SLASH.equals(spell.id())) {
            castGoblinAssassinSlash(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_ARCHER_SHOOT.equals(spell.id())) {
            castGoblinArcherShoot(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_ARCHER_THROW_BOMB.equals(spell.id())) {
            castGoblinArcherThrowBomb(caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_SHAMAN_FIREBALL.equals(spell.id())) {
            castGoblinShamanFireball(instance, caster, target, spell);
            return;
        }
        if (SPELL_GOBLIN_SHAMAN_HEAL.equals(spell.id())) {
            castGoblinShamanHeal(caster, spell);
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

    private void castCursedKnightAttackOne(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        if (!playNamedAnimation(caster, "attack_1")) {
            ModelEngineUtil.playBestAttackAnimation(caster);
        }
        caster.setVelocity(new Vector(0, caster.getVelocity().getY(), 0));
        runLater(18L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            playKnightImpactSounds(caster.getLocation());
            dealConeDamageToPlayers(caster, spell.damage(), 5.0, 180.0, 0.15, 0.05);
        });
        runLater(30L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            playKnightImpactSounds(caster.getLocation());
            dealConeDamageToPlayers(caster, spell.damage(), 5.0, 180.0, 0.28, 0.22);
            throwPlayersFrom(caster.getLocation(), 5.0, 1.15, 0.48);
        });
    }

    private void castCursedKnightAttackTwo(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        if (!playNamedAnimation(caster, "attack_2")) {
            ModelEngineUtil.playBestAttackAnimation(caster);
        }
        caster.setVelocity(new Vector(0, caster.getVelocity().getY(), 0));
        runLater(40L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            Location center = caster.getLocation().clone();
            playKnightImpactSounds(center);
            dealConeDamageToPlayers(caster, spell.damage(), 5.0, 180.0, 0.34, 0.24);
            spawnRingVfx(center, VFX_CURSED_FLAMES, 14, 4.0);
            scheduleAreaPulseSeries(caster, center.clone().add(0.0, 0.1, 0.0),
                    List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                    15L, 1.5, true, 100, null);
            throwPlayersFrom(center, 5.0, 1.35, 0.48);
        });
    }

    private void castCursedKnightAttackThree(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        if (!isCombatContextValid(caster, target)) {
            return;
        }
        if (!playNamedAnimation(caster, "attack_3")) {
            ModelEngineUtil.playBestShootAnimation(caster);
        }
        caster.setVelocity(new Vector(0, caster.getVelocity().getY(), 0));
        playKnightCastSounds(caster.getLocation());
        runLater(12L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            for (int i = 0; i < 5; i++) {
                long delay = i * 2L;
                runLater(delay, () -> spawnRayBurst(caster, spell));
            }
        });
    }

    private void spawnRayBurst(Mob caster, CustomMobDefinition.CustomMobSpell spell) {
        if (caster == null || caster.isDead() || caster.getWorld() == null) {
            return;
        }
        Location strike = randomRingLocation(caster.getLocation(), 4.0, 10.0);
        spawnTemporaryModelVfx(strike, VFX_CURSED_RAY, VFX_LIFETIME_TICKS);
        runLater(12L, () -> scheduleAreaPulseSeries(caster, strike,
                List.of(Math.max(0.1, spell.damage()), 1.0, 1.0, 1.0, 1.0),
                10L, 2.0, true, 100, () -> playRayHum(strike)));
    }

    private void castCursedArcherShootOne(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "shoot_1");
        faceTarget(caster, target);
        runLater(20L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            playArcherShootSounds(caster.getLocation());
            castArrowShot(caster, target, spell);
        });
    }

    private void castCursedArcherShootTwo(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "shoot_2");
        runLater(26L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            playArcherShootSounds(caster.getLocation());
            startArrowRainVolley(caster, target, Math.max(6, (int) Math.round(spell.damage())), 2L,
                    Math.max(0.1, spell.damage() * 0.55));
        });
    }

    private void castCursedArcherShootThree(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "shoot_3");
        faceTarget(caster, target);
        runLater(45L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            playArcherSpecialSounds(caster.getLocation());
            launchModelProjectile(caster, target, VFX_CURSED_ARROW, Math.max(1.5, spell.speed()),
                    Math.max(0.1, spell.damage()), spell.burnTicks(), true);
        });
    }

    private void castCursedMageSpellOne(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        castCursedMageBurst(caster, target, spell, "attack_1", 20L, 2, 15L, VFX_CURSED_CAST,
                Math.max(0.1, spell.damage()), Math.max(0, spell.burnTicks()), Math.max(1.15, spell.speed()));
    }

    private void castCursedMageSpellTwo(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        castCursedMageBurst(caster, target, spell, "attack_1", 20L, 2, 15L, VFX_CURSED_SLASH,
                Math.max(0.1, spell.damage()), Math.max(0, spell.burnTicks()), Math.max(1.2, spell.speed()));
    }

    private void castCursedMageSpellThree(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        castCursedMageBurst(caster, target, spell, "holow_attack", 40L, 1, 1L, VFX_CURSED_HOLLOW,
                Math.max(0.1, spell.damage() * 2.0), Math.max(0, spell.burnTicks()), Math.max(0.8, spell.speed() * 0.7));
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

    private void castGoblinWarriorSwordSlam(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "heavy_swing");
        runLater(37L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            dashTowardTarget(caster, target, 0.52);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITCH_THROW, 0.75f, 0.2f);
            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 0.7f, 0.8f);
            dealConeDamageToPlayers(caster, Math.max(0.1, spell.damage()), 3.2, 90.0, 0.18, 0.12);
            spawnGroundImpact(caster.getLocation());
        });
    }

    private void castGoblinWarriorShieldRush(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "shield_charge");
        runLater(21L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_AMBIENT, 0.75f, 0.8f);
            startRush(caster, target, 32, 0.35, 2.0, Math.max(0.1, spell.damage()));
        });
    }

    private void castGoblinAssassinShadowstep(Mob caster, Player target) {
        if (!isCombatContextValid(caster, target)) {
            return;
        }
        Location to = target.getLocation().clone().add(randomOffset(1.5), 0.0, randomOffset(1.5));
        caster.teleport(to);
        spawnSmoke(caster.getLocation(), 20);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 0.8f, 1.6f);
    }

    private void castGoblinAssassinStab(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "stab");
        runLater(9L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            dashTowardTarget(caster, target, 0.55);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITCH_THROW, 0.75f, 1.1f);
            dealConeDamageToPlayers(caster, Math.max(0.1, spell.damage()), 2.2, 55.0, 0.22, 0.08);
            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.75f, 0.8f);
        });
    }

    private void castGoblinAssassinSlash(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "slash");
        runLater(9L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            dashTowardTarget(caster, target, 0.52);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITCH_THROW, 0.75f, 1.1f);
            dealConeDamageToPlayers(caster, Math.max(0.1, spell.damage() * 0.75), 2.0, 70.0, 0.2, 0.07);
        });
        runLater(19L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            dashTowardTarget(caster, target, 0.52);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITCH_THROW, 0.75f, 1.1f);
            dealConeDamageToPlayers(caster, Math.max(0.1, spell.damage() * 0.75), 2.0, 70.0, 0.2, 0.07);
            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.75f, 0.8f);
        });
    }

    private void castGoblinArcherShoot(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "shoot");
        runLater(35L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 0.8f, 1f);
            caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8f, 1.3f);
            castArrowShot(caster, target, spell);
        });
    }

    private void castGoblinArcherThrowBomb(Mob caster, Player target, CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "throw_bomb");
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 0.75f, 1.7f);
        runLater(22L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            faceTarget(caster, target);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITCH_THROW, 0.8f, 1.0f);
            Location impact = target.getLocation().clone();
            runLater(20L, () -> {
                if (impact.getWorld() == null) {
                    return;
                }
                impact.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, impact.clone().add(0.0, 0.2, 0.0), 1);
                impact.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, impact.clone().add(0.0, 0.2, 0.0), 20, 0.45, 0.2, 0.45, 0.02);
                impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
                for (Player player : getNearbyPlayers(impact, 5.0)) {
                    player.damage(Math.max(0.1, spell.damage() * 1.5), caster);
                }
            });
        });
    }

    private void castGoblinShamanFireball(CustomMobInstance instance,
                                          Mob caster,
                                          Player target,
                                          CustomMobDefinition.CustomMobSpell spell) {
        playNamedAnimation(caster, "shoot");
        runLater(28L, () -> {
            if (!isCombatContextValid(caster, target)) {
                return;
            }
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.9f);
            castMageFireball(instance, caster, target, spell);
        });
    }

    private void castGoblinShamanHeal(Mob caster, CustomMobDefinition.CustomMobSpell spell) {
        if (caster == null || caster.isDead() || caster.getWorld() == null) {
            return;
        }
        playNamedAnimation(caster, "heal");
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_AMBIENT, 0.75f, 0.8f);
        runLater(21L, () -> {
            if (caster.isDead() || caster.getWorld() == null) {
                return;
            }
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.85f, 1.3f);
            for (LivingEntity ally : getNearbyAlliedCustomMobs(caster, 20.0, def -> def.id().toLowerCase().startsWith("goblin_"), 4)) {
                double healAmount = Math.max(4.0, spell.damage());
                var maxHealthAttribute = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
                double maxHealth = maxHealthAttribute != null && ally.getAttribute(maxHealthAttribute) != null
                        ? ally.getAttribute(maxHealthAttribute).getValue()
                        : ally.getHealth();
                ally.setHealth(Math.min(maxHealth, ally.getHealth() + healAmount));
                ally.getWorld().spawnParticle(org.bukkit.Particle.DUST_COLOR_TRANSITION, ally.getLocation().add(0.0, 0.9, 0.0),
                        16, 0.3, 0.6, 0.3, 0.0,
                        new org.bukkit.Particle.DustTransition(org.bukkit.Color.fromRGB(0xE9FF26), org.bukkit.Color.fromRGB(0x2BFF99), 0.8f));
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
        return ModelEngineUtil.playAnimationByName(caster, animationName, false);
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

    private void playKnightImpactSounds(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.1f);
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.5f);
        at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.95f, 0.9f);
        at.getWorld().playSound(at, Sound.ITEM_AXE_STRIP, 1.0f, 0.8f);
    }

    private void playKnightCastSounds(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        at.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.5f);
        at.getWorld().playSound(at, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.85f, 0.95f);
        at.getWorld().playSound(at, Sound.ITEM_AXE_STRIP, 1.0f, 0.8f);
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

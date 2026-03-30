package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.mob.custom.spawner.CustomMobSpawnerManager;
import me.nakilex.levelplugin.mob.custom.gui.CustomMobAdminGUI;
import me.nakilex.levelplugin.mob.custom.spells.CustomMobSpellController;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.ArcPattern;
import me.nakilex.levelplugin.particles.patterns.ParticlePattern;
import me.nakilex.levelplugin.particles.patterns.RingPattern;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.Comparator;

public class CustomMobManager {
    public static final String CUSTOM_MOB_ID_META = "lp_custom_mob_id";
    public static final String CUSTOM_MOB_TAG = "custom_mob";

    private static final List<String> DEFAULT_EXAMPLES = List.of(
            "custom_mobs/cursed_archer.yml",
            "custom_mobs/forest_slime.yml",
            "custom_mobs/moss_zombie.yml",
            "custom_mobs/cave_stalker.yml",
            "custom_mobs/crypt_skeleton.yml",
            "custom_mobs/ember_witch.yml",
            "custom_mobs/reliquary_giant.yml",
            "custom_mobs/rpg_rat.yml",
            "custom_mobs/goblin_archer.yml",
            "custom_mobs/goblin_assassin.yml",
            "custom_mobs/goblin_shaman.yml",
            "custom_mobs/goblin_warrior.yml",
            "custom_mobs/cursed_arrow.yml",
            "custom_mobs/slime_king.yml",
            "custom_mobs/wild_rooster.yml",
            "custom_mobs/cursed_hollow.yml",
            "custom_mobs/cursed_knight.yml",
            "custom_mobs/cursed_mage.yml",
            "custom_mobs/cursed_night.yml",
            "custom_mobs/mso_magma_imp.yml",
            "custom_mobs/vp1_hermit_crab.yml",
            "custom_mobs/vp1_golem_damaged_1.yml"
    );
    private static final List<String> DEFAULT_SPELL_SCRIPTS = List.of(
            "custom_mob_spells/cursed_archer_shoot_1.yml",
            "custom_mob_spells/cursed_archer_shoot_2.yml",
            "custom_mob_spells/cursed_archer_shoot_3.yml",
            "custom_mob_spells/cursed_mage_spell_1.yml",
            "custom_mob_spells/cursed_mage_spell_2.yml",
            "custom_mob_spells/cursed_mage_spell_3.yml",
            "custom_mob_spells/cursed_knight_attack_1.yml",
            "custom_mob_spells/cursed_knight_attack_2.yml",
            "custom_mob_spells/cursed_knight_attack_3.yml",
            "custom_mob_spells/goblin_archer_shoot.yml",
            "custom_mob_spells/goblin_archer_throw_bomb.yml",
            "custom_mob_spells/goblin_assassin_shadowstep.yml",
            "custom_mob_spells/goblin_assassin_stab.yml",
            "custom_mob_spells/goblin_assassin_slash.yml",
            "custom_mob_spells/goblin_warrior_sword_slam.yml",
            "custom_mob_spells/goblin_warrior_shield_rush.yml",
            "custom_mob_spells/goblin_shaman_fireball.yml",
            "custom_mob_spells/goblin_shaman_heal.yml"
    );

    private static final ArcPattern STUN_PATTERN = new ArcPattern(
            Particle.CRIT,
            null,
            0.55,
            0,
            70,
            16,
            ParticlePlane.Y,
            0,
            ParticleRotationAxis.Y
    );
    private static final RingPattern POISON_PATTERN = new RingPattern(
            Particle.WITCH,
            null,
            0.55,
            12,
            ParticlePlane.Y,
            0,
            ParticleRotationAxis.Y
    );
    private static final RingPattern TAUNT_PATTERN = new RingPattern(
            Particle.ANGRY_VILLAGER,
            null,
            0.55,
            10,
            ParticlePlane.Y,
            0,
            ParticleRotationAxis.Y
    );
    private static final RingPattern FEAR_PATTERN = new RingPattern(
            Particle.SMOKE,
            null,
            0.55,
            10,
            ParticlePlane.Y,
            0,
            ParticleRotationAxis.Y
    );
    private static final RingPattern SLOW_PATTERN = new RingPattern(
            Particle.CLOUD,
            null,
            0.55,
            8,
            ParticlePlane.Y,
            0,
            ParticleRotationAxis.Y
    );
    private static final Map<CustomMobStatus, ParticlePattern> STATUS_PATTERNS = buildStatusPatterns();
    private static final Map<CustomMobStatus, Integer> STATUS_POINTS = buildStatusPoints();
    private static final double STATUS_HEIGHT_OFFSET = 0.35;
    private static final double FEAR_SPEED_MULTIPLIER = 0.0875;
    private static final int SLOW_AMPLIFIER = 1;

    private final Main plugin;
    private final CustomMobNameManager nameManager;
    private final CustomMobSpawnerManager spawnerManager;
    private final CustomMobAdminGUI adminGui;
    private final CustomMobSpellController spellController;
    private final Map<String, CustomMobDefinition> definitions = new HashMap<>();
    private final Map<UUID, CustomMobInstance> activeMobs = new HashMap<>();
    private final Random random = new Random();

    public CustomMobManager(Main plugin) {
        this.plugin = plugin;
        this.nameManager = new CustomMobNameManager(plugin, this);
        this.spawnerManager = new CustomMobSpawnerManager(plugin, this);
        this.adminGui = new CustomMobAdminGUI(plugin, this, spawnerManager);
        this.spellController = new CustomMobSpellController(plugin, this);
        loadDefinitions();
    }

    public CustomMobNameManager getNameManager() {
        return nameManager;
    }

    public CustomMobSpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public CustomMobAdminGUI getAdminGui() {
        return adminGui;
    }

    public List<String> getMobIds() {
        return getDefinitionsByProgression().stream()
                .map(CustomMobDefinition::id)
                .toList();
    }

    public List<CustomMobDefinition> getDefinitionsByProgression() {
        return definitions.values().stream()
                .sorted(Comparator
                        .comparingInt((CustomMobDefinition def) -> def.levelRange().min())
                        .thenComparingInt(def -> def.levelRange().max())
                        .thenComparingInt(this::resolvePowerScore)
                        .thenComparing(CustomMobDefinition::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<CustomMobDefinition> getDefinition(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<CustomMobInstance> getInstance(UUID uuid) {
        return Optional.ofNullable(activeMobs.get(uuid));
    }

    public Optional<CustomMobInstance> getInstance(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return getInstance(entity.getUniqueId());
    }

    public Map<UUID, CustomMobInstance> getActiveMobs() {
        return activeMobs;
    }

    public boolean stun(LivingEntity entity, int durationTicks) {
        return applyStatus(entity, CustomMobStatus.STUNNED, durationTicks, null);
    }

    public boolean poison(LivingEntity entity, int durationTicks) {
        return applyStatus(entity, CustomMobStatus.POISONED, durationTicks, null);
    }

    public boolean taunt(LivingEntity entity, Player source, int durationTicks) {
        return applyStatus(entity, CustomMobStatus.TAUNTED, durationTicks, source);
    }

    public boolean fear(LivingEntity entity, Player source, int durationTicks) {
        return applyStatus(entity, CustomMobStatus.FEARED, durationTicks, source);
    }

    public boolean slow(LivingEntity entity, int durationTicks) {
        return applyStatus(entity, CustomMobStatus.SLOWED, durationTicks, null);
    }

    public void reload() {
        loadDefinitions();
        spellController.reload();
    }

    public List<LivingEntity> spawn(String id, Location location, int amount) {
        return spawn(id, location, amount, null);
    }

    public List<LivingEntity> spawn(String id, Location location, int amount, Integer levelOverride) {
        Optional<CustomMobDefinition> defOpt = getDefinition(id);
        if (defOpt.isEmpty() || location == null || location.getWorld() == null) {
            return List.of();
        }
        CustomMobDefinition definition = defOpt.get();
        int count = Math.max(1, amount);
        List<LivingEntity> spawned = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            var raw = location.getWorld().spawnEntity(location, definition.entityType());
            if (!(raw instanceof LivingEntity entity)) {
                if (raw != null) {
                    raw.remove();
                }
                continue;
            }
            int level = levelOverride != null
                    ? Math.max(1, levelOverride)
                    : definition.levelRange().pickLevel(random);
            applyDefinition(entity, definition);
            CustomMobInstance instance = new CustomMobInstance(definition, entity, level);
            activeMobs.put(entity.getUniqueId(), instance);
            nameManager.track(instance);
            spawned.add(entity);
        }
        return spawned;
    }

    public void remove(UUID uuid) {
        CustomMobInstance instance = activeMobs.remove(uuid);
        if (instance != null) {
            instance.clearAllStatusTasks();
            spellController.clearMob(uuid);
            nameManager.untrack(uuid);
            spawnerManager.removeActiveMob(uuid);
        }
    }

    private void loadDefinitions() {
        definitions.clear();
        File folder = new File(plugin.getDataFolder(), "custom_mobs");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Failed to create custom_mobs folder.");
        }
        for (String resource : DEFAULT_EXAMPLES) {
            File target = new File(plugin.getDataFolder(), resource);
            if (!target.exists() && plugin.getResource(resource) != null) {
                plugin.saveResource(resource, false);
            }
        }
        for (String resource : DEFAULT_SPELL_SCRIPTS) {
            File target = new File(plugin.getDataFolder(), resource);
            if (!target.exists() && plugin.getResource(resource) != null) {
                plugin.saveResource(resource, false);
            }
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String base = file.getName().substring(0, file.getName().length() - ".yml".length());
            var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            CustomMobDefinition def = CustomMobDefinition.fromConfig(base, cfg);
            definitions.put(def.id().toLowerCase(Locale.ROOT), def);
        }
    }

    private void applyDefinition(LivingEntity entity, CustomMobDefinition definition) {
        entity.addScoreboardTag(CUSTOM_MOB_TAG);
        entity.setMetadata(CUSTOM_MOB_ID_META, new FixedMetadataValue(plugin, definition.id()));
        CustomMobDefinition.CustomMobOptions options = definition.options();
        if (options == null) {
            options = new CustomMobDefinition.CustomMobOptions(null, null, null, null, null, true, false, false);
        }
        entity.setAI(options.ai());
        entity.setSilent(options.silent());
        entity.setRemoveWhenFarAway(options.despawn());
        if (entity instanceof Ageable ageable) {
            ageable.setAdult();
        }
        if (entity instanceof Slime slime && definition.id().equalsIgnoreCase("slime_king")) {
            slime.setSize(120);
        }
        CustomMobStats stats = definition.stats();
        double baseHealth = definition.baseHealth() != null
                ? definition.baseHealth()
                : me.nakilex.levelplugin.player.attributes.managers.StatsManager.BASE_HEALTH;
        double maxHealth = stats.computeMaxHealth(baseHealth);
        Attribute maxHealthAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        if (maxHealthAttr != null && entity.getAttribute(maxHealthAttr) != null) {
            entity.getAttribute(maxHealthAttr).setBaseValue(maxHealth);
        }
        entity.setHealth(maxHealth);
        Double moveSpeed = options.movementSpeed() != null
                ? options.movementSpeed()
                : 0.2 + stats.agility() * 0.002;
        Double attackDamage = options.attackDamage() != null
                ? options.attackDamage()
                : (stats.strength() > 0 ? 1.0 + stats.strength() * 0.5 : null);
        Double attackSpeed = options.attackSpeed() != null
                ? options.attackSpeed()
                : (stats.technique() > 0 ? 0.5 * (1.0 + 0.0075 * stats.technique()) * 8.0 : null);

        applyAttribute(entity, AttributeUtil.resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED"), moveSpeed);
        applyAttribute(entity, AttributeUtil.resolve("GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE"), options.followRange());
        applyAttribute(entity, AttributeUtil.resolve("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE"), options.knockbackResistance());
        applyAttribute(entity, AttributeUtil.resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE"), attackDamage);
        applyAttribute(entity, AttributeUtil.resolve("GENERIC_ATTACK_SPEED", "ATTACK_SPEED"), attackSpeed);

        if (Bukkit.getPluginManager().isPluginEnabled("ModelEngine") && !definition.models().isEmpty()) {
            ModelEngineUtil.ModelApplyResult result = ModelEngineUtil.applyModels(entity, definition.models(), plugin);
            if (!result.failed().isEmpty()) {
                plugin.getLogger().warning("Mob " + definition.id()
                        + " failed to apply ModelEngine models: " + String.join(", ", result.failed()));
            }
        }
    }

    private int resolvePowerScore(CustomMobDefinition definition) {
        if (definition == null || definition.stats() == null) {
            return 0;
        }
        CustomMobStats stats = definition.stats();
        int offense = stats.strength() + stats.intelligence() + stats.dexterity() + stats.technique();
        int defense = stats.vitality() + stats.will();
        int score = offense * 2 + defense;
        if (definition.boss()) {
            score += 500;
        }
        return score;
    }

    private void applyAttribute(LivingEntity entity, Attribute attr, Double value) {
        if (attr == null || value == null || entity.getAttribute(attr) == null) {
            return;
        }
        entity.getAttribute(attr).setBaseValue(value);
    }

    private boolean applyStatus(LivingEntity entity, CustomMobStatus status, int durationTicks, Player source) {
        if (entity == null) {
            return false;
        }
        CustomMobInstance instance = getInstance(entity).orElse(null);
        if (instance == null) {
            return false;
        }
        if ((status == CustomMobStatus.TAUNTED || status == CustomMobStatus.FEARED) && source == null) {
            return false;
        }
        int ticks = Math.max(1, durationTicks);
        if (instance.isStatusActive(status)) {
            instance.clearStatusTasks(status);
        }
        instance.setStatusActive(status, true);
        if (source != null) {
            instance.setStatusSource(status, source.getUniqueId());
        } else {
            instance.setStatusSource(status, null);
        }
        switch (status) {
            case STUNNED -> {
                entity.setAI(false);
                entity.setVelocity(entity.getVelocity().setY(0));
            }
            case TAUNTED -> startTauntTask(instance, source);
            case FEARED -> startFearTask(instance, source);
            case SLOWED -> PotionEffectUtil.applyHiddenEffect(entity, PotionEffectType.SLOWNESS, ticks, SLOW_AMPLIFIER);
            case POISONED -> startPoisonDamageTask(instance, ticks);
        }
        BukkitTask particleTask = startStatusParticles(instance, status);
        if (particleTask != null) {
            instance.setParticleTask(status, particleTask);
        }
        BukkitTask resetTask = Bukkit.getScheduler().runTaskLater(plugin, () -> endStatus(instance, status), ticks);
        instance.setResetTask(status, resetTask);
        return true;
    }

    private void endStatus(CustomMobInstance instance, CustomMobStatus status) {
        LivingEntity entity = instance.entity();
        if (entity == null) {
            instance.clearStatusTasks(status);
            return;
        }
        switch (status) {
            case STUNNED -> {
                if (!entity.isDead()) {
                    entity.setAI(instance.baseAi());
                }
            }
            case TAUNTED -> {
                if (!entity.isDead() && entity instanceof Mob mob) {
                    instance.getStatusSource(status)
                            .map(Bukkit::getPlayer)
                            .filter(player -> mob.getTarget() != null && mob.getTarget().equals(player))
                            .ifPresent(player -> mob.setTarget(null));
                }
            }
            case FEARED, POISONED -> {
            }
            case SLOWED -> {
                if (!entity.isDead()) {
                    PotionEffectUtil.removeEffect(entity, PotionEffectType.SLOWNESS);
                }
            }
        }
        instance.clearStatusTasks(status);
    }

    private BukkitTask startStatusParticles(CustomMobInstance instance, CustomMobStatus status) {
        ParticlePattern pattern = STATUS_PATTERNS.get(status);
        if (pattern == null) {
            return null;
        }
        return new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                LivingEntity entity = instance.entity();
                if (entity == null || entity.isDead() || !instance.isStatusActive(status)) {
                    cancel();
                    return;
                }
                Location base = entity.getLocation();
                Location center = base.clone().add(0, entity.getHeight() + STATUS_HEIGHT_OFFSET, 0);
                int points = STATUS_POINTS.getOrDefault(status, 1);
                ParticleRenderContext context = new ParticleRenderContext(null, center, base, points, tick, 20);
                pattern.render(context);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startPoisonDamageTask(CustomMobInstance instance, int durationTicks) {
        LivingEntity entity = instance.entity();
        if (entity == null || entity.isDead()) {
            return;
        }
        int totalSeconds = (int) Math.ceil(durationTicks / 20.0);
        BukkitRunnable runnable = new BukkitRunnable() {
            private int remainingSeconds = totalSeconds;

            @Override
            public void run() {
                LivingEntity living = instance.entity();
                if (living == null || living.isDead() || !instance.isStatusActive(CustomMobStatus.POISONED)) {
                    cancel();
                    return;
                }
                if (remainingSeconds <= 0) {
                    cancel();
                    return;
                }
                double maxHealth = resolveMaxHealth(living);
                double damage = Math.max(0.1, maxHealth * 0.01);
                living.damage(damage);
                remainingSeconds--;
            }
        };
        instance.setEffectTask(CustomMobStatus.POISONED, runnable.runTaskTimer(plugin, 20L, 20L));
    }

    private void startTauntTask(CustomMobInstance instance, Player source) {
        if (source == null) {
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity entity = instance.entity();
                if (entity == null || entity.isDead() || !instance.isStatusActive(CustomMobStatus.TAUNTED)) {
                    cancel();
                    return;
                }
                Player player = source.isOnline() ? source : null;
                if (player == null) {
                    cancel();
                    return;
                }
                if (entity instanceof Mob mob) {
                    mob.setTarget(player);
                }
            }
        };
        instance.setEffectTask(CustomMobStatus.TAUNTED, runnable.runTaskTimer(plugin, 0L, 10L));
    }

    private void startFearTask(CustomMobInstance instance, Player source) {
        if (source == null) {
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity entity = instance.entity();
                if (entity == null || entity.isDead() || !instance.isStatusActive(CustomMobStatus.FEARED)) {
                    cancel();
                    return;
                }
                Player player = source.isOnline() ? source : null;
                if (player == null) {
                    cancel();
                    return;
                }
                Vector away = entity.getLocation().toVector().subtract(player.getLocation().toVector());
                if (away.lengthSquared() < 0.0001) {
                    return;
                }
                away.normalize().multiply(FEAR_SPEED_MULTIPLIER).setY(0);
                entity.setVelocity(away);
                if (entity instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
        };
        instance.setEffectTask(CustomMobStatus.FEARED, runnable.runTaskTimer(plugin, 0L, 1L));
    }

    private static Map<CustomMobStatus, ParticlePattern> buildStatusPatterns() {
        Map<CustomMobStatus, ParticlePattern> patterns = new EnumMap<>(CustomMobStatus.class);
        patterns.put(CustomMobStatus.STUNNED, STUN_PATTERN);
        patterns.put(CustomMobStatus.POISONED, POISON_PATTERN);
        patterns.put(CustomMobStatus.TAUNTED, TAUNT_PATTERN);
        patterns.put(CustomMobStatus.FEARED, FEAR_PATTERN);
        patterns.put(CustomMobStatus.SLOWED, SLOW_PATTERN);
        return patterns;
    }

    private static Map<CustomMobStatus, Integer> buildStatusPoints() {
        Map<CustomMobStatus, Integer> points = new EnumMap<>(CustomMobStatus.class);
        points.put(CustomMobStatus.STUNNED, 6);
        points.put(CustomMobStatus.POISONED, 2);
        points.put(CustomMobStatus.TAUNTED, 2);
        points.put(CustomMobStatus.FEARED, 2);
        points.put(CustomMobStatus.SLOWED, 2);
        return points;
    }

    private double resolveMaxHealth(LivingEntity entity) {
        Attribute maxHealthAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        if (maxHealthAttr != null && entity.getAttribute(maxHealthAttr) != null) {
            return entity.getAttribute(maxHealthAttr).getValue();
        }
        return entity.getHealth();
    }
}

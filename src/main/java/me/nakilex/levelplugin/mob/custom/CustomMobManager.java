package me.nakilex.levelplugin.mob.custom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.mob.custom.spawner.CustomMobSpawnerManager;
import me.nakilex.levelplugin.mob.custom.gui.CustomMobAdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class CustomMobManager {
    public static final String CUSTOM_MOB_ID_META = "lp_custom_mob_id";
    public static final String CUSTOM_MOB_TAG = "custom_mob";

    private static final List<String> DEFAULT_EXAMPLES = List.of(
            "custom_mobs/cursed_archer.yml",
            "custom_mobs/forest_slime.yml"
    );

    private final Main plugin;
    private final CustomMobNameManager nameManager;
    private final CustomMobSpawnerManager spawnerManager;
    private final CustomMobAdminGUI adminGui;
    private final Map<String, CustomMobDefinition> definitions = new HashMap<>();
    private final Map<UUID, CustomMobInstance> activeMobs = new HashMap<>();
    private final Random random = new Random();

    public CustomMobManager(Main plugin) {
        this.plugin = plugin;
        this.nameManager = new CustomMobNameManager(plugin, this);
        this.spawnerManager = new CustomMobSpawnerManager(plugin, this);
        this.adminGui = new CustomMobAdminGUI(plugin, this, spawnerManager);
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
        return definitions.values().stream()
                .map(CustomMobDefinition::id)
                .sorted(String.CASE_INSENSITIVE_ORDER)
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

    public void reload() {
        loadDefinitions();
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
            if (!target.exists()) {
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
                plugin.getLogger().warning("Custom mob " + definition.id()
                        + " failed to apply ModelEngine models: " + String.join(", ", result.failed()));
            }
        }
    }

    private void applyAttribute(LivingEntity entity, Attribute attr, Double value) {
        if (attr == null || value == null || entity.getAttribute(attr) == null) {
            return;
        }
        entity.getAttribute(attr).setBaseValue(value);
    }
}

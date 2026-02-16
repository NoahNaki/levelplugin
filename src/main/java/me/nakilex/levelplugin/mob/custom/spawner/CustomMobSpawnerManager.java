package me.nakilex.levelplugin.mob.custom.spawner;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import me.nakilex.levelplugin.utils.AttributeUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomMobSpawnerManager implements Listener {
    private static final String CONFIG_FILE = "custom_mob_spawners.yml";

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final Map<String, CustomMobSpawner> spawners = new HashMap<>();
    private final Random random = new Random();
    private BukkitTask ticker;

    public CustomMobSpawnerManager(Main plugin, CustomMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        load();
        startTicker();
    }

    public List<String> getSpawnerNames() {
        return spawners.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Optional<CustomMobSpawner> getSpawner(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(spawners.get(name.toLowerCase(Locale.ROOT)));
    }

    public boolean createSpawner(String name, String mobId, Location location) {
        if (name == null || name.isBlank() || mobId == null || mobId.isBlank()) {
            return false;
        }
        if (spawners.containsKey(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        CustomMobSpawner spawner = new CustomMobSpawner(name, mobId, location);
        spawners.put(name.toLowerCase(Locale.ROOT), spawner);
        save();
        return true;
    }

    public boolean removeSpawner(String name) {
        if (name == null) {
            return false;
        }
        CustomMobSpawner removed = spawners.remove(name.toLowerCase(Locale.ROOT));
        if (removed != null) {
            removed.getActiveMobs().clear();
            save();
            return true;
        }
        return false;
    }

    public List<String> getFlagNames() {
        return List.of(
                "mobname",
                "world",
                "spawnergroup",
                "x",
                "y",
                "z",
                "radius",
                "radiusy",
                "usetimer",
                "maxmobs",
                "moblevel",
                "mobsperspawn",
                "cooldown",
                "cooldowntimer",
                "warmup",
                "warmuptimer",
                "checkforplayers",
                "activationrange",
                "leashrange",
                "healonleash",
                "resetthreatonleash",
                "showflames",
                "breakable",
                "fieldboss",
                "conditions"
        );
    }

    public ChatMessageUtil.MessageType setFlag(String name, String flag, String value) {
        Optional<CustomMobSpawner> opt = getSpawner(name);
        if (opt.isEmpty()) {
            return ChatMessageUtil.MessageType.ERROR;
        }
        CustomMobSpawner spawner = opt.get();
        String normalized = flag.toLowerCase(Locale.ROOT);
        try {
            switch (normalized) {
                case "mobname" -> {
                    if (mobManager.getDefinition(value).isEmpty()) {
                        return ChatMessageUtil.MessageType.ERROR;
                    }
                    spawner.setMobId(value);
                }
                case "world" -> {
                    if (Bukkit.getWorld(value) == null) {
                        return ChatMessageUtil.MessageType.ERROR;
                    }
                    spawner.setWorld(value);
                }
                case "spawnergroup" -> spawner.setSpawnerGroup(value);
                case "x" -> spawner.setX(Double.parseDouble(value));
                case "y" -> spawner.setY(Double.parseDouble(value));
                case "z" -> spawner.setZ(Double.parseDouble(value));
                case "radius" -> spawner.setRadius(Double.parseDouble(value));
                case "radiusy" -> spawner.setRadiusY(Double.parseDouble(value));
                case "usetimer" -> spawner.setUseTimer(Boolean.parseBoolean(value));
                case "maxmobs" -> spawner.setMaxMobs(Integer.parseInt(value));
                case "moblevel" -> {
                    if ("default".equalsIgnoreCase(value) || "clear".equalsIgnoreCase(value)) {
                        spawner.setMobLevel(null);
                    } else {
                        spawner.setMobLevel(Integer.parseInt(value));
                    }
                }
                case "mobsperspawn" -> spawner.setMobsPerSpawn(Integer.parseInt(value));
                case "cooldown" -> spawner.setCooldown(Integer.parseInt(value));
                case "cooldowntimer" -> spawner.setCooldownTimer(Integer.parseInt(value));
                case "warmup" -> spawner.setWarmup(Integer.parseInt(value));
                case "warmuptimer" -> spawner.setWarmupTimer(Integer.parseInt(value));
                case "checkforplayers" -> spawner.setCheckForPlayers(Boolean.parseBoolean(value));
                case "activationrange" -> spawner.setActivationRange(Double.parseDouble(value));
                case "leashrange" -> spawner.setLeashRange(Double.parseDouble(value));
                case "healonleash" -> spawner.setHealOnLeash(Boolean.parseBoolean(value));
                case "resetthreatonleash" -> spawner.setResetThreatOnLeash(Boolean.parseBoolean(value));
                case "showflames" -> spawner.setShowFlames(Boolean.parseBoolean(value));
                case "breakable" -> spawner.setBreakable(Boolean.parseBoolean(value));
                case "fieldboss" -> spawner.setFieldBoss(Boolean.parseBoolean(value));
                case "conditions" -> {
                    List<String> list = value.isBlank()
                            ? Collections.emptyList()
                            : List.of(value.split(","));
                    spawner.setConditions(list.stream()
                            .map(String::trim)
                            .filter(v -> !v.isEmpty())
                            .toList());
                }
                default -> {
                    return ChatMessageUtil.MessageType.ERROR;
                }
            }
        } catch (NumberFormatException ex) {
            return ChatMessageUtil.MessageType.ERROR;
        }
        save();
        return ChatMessageUtil.MessageType.SUCCESS;
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
        }
        spawners.values().forEach(spawner -> spawner.getActiveMobs().clear());
    }

    @EventHandler
    public void onCustomMobDeath(EntityDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        spawners.values().forEach(spawner -> spawner.getActiveMobs().remove(uuid));
    }

    private void startTicker() {
        if (ticker != null) {
            ticker.cancel();
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpawners, 20L, 20L);
    }

    private void tickSpawners() {
        for (CustomMobSpawner spawner : spawners.values()) {
            World world = Bukkit.getWorld(spawner.getWorld());
            if (world == null) {
                continue;
            }
            pruneActive(spawner, world);
            if (spawner.isShowFlames()) {
                Location center = new Location(world, spawner.getX(), spawner.getY(), spawner.getZ());
                world.spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 0), 4, 0.2, 0.2, 0.2, 0.0);
            }
            if (spawner.isCheckForPlayers() && !hasPlayersNearby(spawner, world)) {
                continue;
            }
            if (spawner.isUseTimer()) {
                if (spawner.getWarmupTimer() > 0) {
                    spawner.setWarmupTimer(spawner.getWarmupTimer() - 1);
                    continue;
                }
                if (spawner.getCooldownTimer() > 0) {
                    spawner.setCooldownTimer(spawner.getCooldownTimer() - 1);
                    continue;
                }
            }
            int available = spawner.getMaxMobs() - spawner.getActiveMobs().size();
            if (available <= 0) {
                continue;
            }
            int toSpawn = Math.min(spawner.getMobsPerSpawn(), available);
            for (int i = 0; i < toSpawn; i++) {
                Location spawn = randomSpawnLocation(spawner, world);
                List<LivingEntity> spawned = mobManager.spawn(
                        spawner.getMobId(),
                        spawn,
                        1,
                        spawner.getMobLevel());
                if (!spawned.isEmpty()) {
                    LivingEntity entity = spawned.get(0);
                    if (spawner.isFieldBoss()) {
                        entity.addScoreboardTag("field_boss");
                    }
                    spawner.getActiveMobs().add(entity.getUniqueId());
                }
            }
            if (spawner.isUseTimer()) {
                spawner.setCooldownTimer(spawner.getCooldown());
                if (spawner.getWarmup() > 0 && spawner.getWarmupTimer() <= 0) {
                    spawner.setWarmupTimer(spawner.getWarmup());
                }
            }
        }
    }

    private void pruneActive(CustomMobSpawner spawner, World world) {
        Location center = new Location(world, spawner.getX(), spawner.getY(), spawner.getZ());
        Set<UUID> active = spawner.getActiveMobs();
        active.removeIf(id -> {
            var entity = world.getEntity(id);
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                return true;
            }
            if (spawner.getLeashRange() > 0) {
                double dist = living.getLocation().distance(center);
                if (dist > spawner.getLeashRange()) {
                    living.teleport(center);
                    if (spawner.isHealOnLeash()) {
                        Attribute maxHealthAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
                        AttributeInstance maxHealthInst = maxHealthAttr != null ? living.getAttribute(maxHealthAttr) : null;
                        double max = maxHealthInst != null ? maxHealthInst.getValue() : living.getHealth();
                        living.setHealth(max);
                    }
                    if (spawner.isResetThreatOnLeash() && living instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                }
            }
            return false;
        });
    }

    private boolean hasPlayersNearby(CustomMobSpawner spawner, World world) {
        Location center = new Location(world, spawner.getX(), spawner.getY(), spawner.getZ());
        double range = spawner.getActivationRange();
        double rangeSq = range * range;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    private Location randomSpawnLocation(CustomMobSpawner spawner, World world) {
        double radius = Math.max(0.0, spawner.getRadius());
        double radiusY = Math.max(0.0, spawner.getRadiusY());
        double dx = radius > 0 ? (random.nextDouble() * 2 - 1) * radius : 0.0;
        double dz = radius > 0 ? (random.nextDouble() * 2 - 1) * radius : 0.0;
        double dy = radiusY > 0 ? (random.nextDouble() * 2 - 1) * radiusY : 0.0;
        return new Location(world, spawner.getX() + dx, spawner.getY() + dy, spawner.getZ() + dz);
    }

    private void load() {
        spawners.clear();
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (!file.exists()) {
            save();
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("spawners");
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            ConfigurationSection spawnerCfg = section.getConfigurationSection(name);
            if (spawnerCfg == null) {
                continue;
            }
            String mobId = spawnerCfg.getString("MobName", "");
            String worldName = spawnerCfg.getString("World", "world");
            World world = Bukkit.getWorld(worldName);
            Location loc = world != null ? world.getSpawnLocation() : new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
            CustomMobSpawner spawner = new CustomMobSpawner(name, mobId, loc);
            spawner.setWorld(worldName);
            spawner.setSpawnerGroup(spawnerCfg.getString("SpawnerGroup", spawner.getSpawnerGroup()));
            spawner.setX(spawnerCfg.getDouble("X", spawner.getX()));
            spawner.setY(spawnerCfg.getDouble("Y", spawner.getY()));
            spawner.setZ(spawnerCfg.getDouble("Z", spawner.getZ()));
            spawner.setRadius(spawnerCfg.getDouble("Radius", spawner.getRadius()));
            spawner.setRadiusY(spawnerCfg.getDouble("RadiusY", spawner.getRadiusY()));
            spawner.setUseTimer(spawnerCfg.getBoolean("UseTimer", spawner.isUseTimer()));
            spawner.setMaxMobs(spawnerCfg.getInt("MaxMobs", spawner.getMaxMobs()));
            if (spawnerCfg.contains("MobLevel")) {
                spawner.setMobLevel(spawnerCfg.getInt("MobLevel"));
            }
            spawner.setMobsPerSpawn(spawnerCfg.getInt("MobsPerSpawn", spawner.getMobsPerSpawn()));
            spawner.setCooldown(spawnerCfg.getInt("Cooldown", spawner.getCooldown()));
            spawner.setCooldownTimer(spawnerCfg.getInt("CooldownTimer", spawner.getCooldownTimer()));
            spawner.setWarmup(spawnerCfg.getInt("Warmup", spawner.getWarmup()));
            spawner.setWarmupTimer(spawnerCfg.getInt("WarmupTimer", spawner.getWarmupTimer()));
            spawner.setCheckForPlayers(spawnerCfg.getBoolean("CheckForPlayers", spawner.isCheckForPlayers()));
            spawner.setActivationRange(spawnerCfg.getDouble("ActivationRange", spawner.getActivationRange()));
            spawner.setLeashRange(spawnerCfg.getDouble("LeashRange", spawner.getLeashRange()));
            spawner.setHealOnLeash(spawnerCfg.getBoolean("HealOnLeash", spawner.isHealOnLeash()));
            spawner.setResetThreatOnLeash(spawnerCfg.getBoolean("ResetThreatOnLeash", spawner.isResetThreatOnLeash()));
            spawner.setShowFlames(spawnerCfg.getBoolean("ShowFlames", spawner.isShowFlames()));
            spawner.setBreakable(spawnerCfg.getBoolean("Breakable", spawner.isBreakable()));
            spawner.setFieldBoss(spawnerCfg.getBoolean("FieldBoss", spawner.isFieldBoss()));
            spawner.setConditions(spawnerCfg.getStringList("Conditions"));
            spawners.put(name.toLowerCase(Locale.ROOT), spawner);
        }
    }

    private void save() {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        FileConfiguration cfg = new YamlConfiguration();
        ConfigurationSection section = cfg.createSection("spawners");
        for (CustomMobSpawner spawner : spawners.values()) {
            ConfigurationSection entry = section.createSection(spawner.getName());
            entry.set("MobName", spawner.getMobId());
            entry.set("World", spawner.getWorld());
            entry.set("SpawnerGroup", spawner.getSpawnerGroup());
            entry.set("X", spawner.getX());
            entry.set("Y", spawner.getY());
            entry.set("Z", spawner.getZ());
            entry.set("Radius", spawner.getRadius());
            entry.set("RadiusY", spawner.getRadiusY());
            entry.set("UseTimer", spawner.isUseTimer());
            entry.set("MaxMobs", spawner.getMaxMobs());
            if (spawner.getMobLevel() != null) {
                entry.set("MobLevel", spawner.getMobLevel());
            }
            entry.set("MobsPerSpawn", spawner.getMobsPerSpawn());
            entry.set("Cooldown", spawner.getCooldown());
            entry.set("CooldownTimer", spawner.getCooldownTimer());
            entry.set("Warmup", spawner.getWarmup());
            entry.set("WarmupTimer", spawner.getWarmupTimer());
            entry.set("CheckForPlayers", spawner.isCheckForPlayers());
            entry.set("ActivationRange", spawner.getActivationRange());
            entry.set("LeashRange", spawner.getLeashRange());
            entry.set("HealOnLeash", spawner.isHealOnLeash());
            entry.set("ResetThreatOnLeash", spawner.isResetThreatOnLeash());
            entry.set("ShowFlames", spawner.isShowFlames());
            entry.set("Breakable", spawner.isBreakable());
            entry.set("FieldBoss", spawner.isFieldBoss());
            entry.set("Conditions", spawner.getConditions());
            entry.set("ActiveMobs", spawner.getActiveMobs().size());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mob spawners: " + e.getMessage());
        }
    }

    public List<String> describeSpawner(CustomMobSpawner spawner) {
        List<String> lines = new ArrayList<>();
        lines.add("Mob: " + spawner.getMobId());
        lines.add("World: " + spawner.getWorld());
        lines.add("Group: " + spawner.getSpawnerGroup());
        lines.add(String.format("Location: %.1f %.1f %.1f", spawner.getX(), spawner.getY(), spawner.getZ()));
        lines.add("Radius: " + spawner.getRadius() + " / " + spawner.getRadiusY());
        lines.add("MaxMobs: " + spawner.getMaxMobs() + " | Active: " + spawner.getActiveMobs().size());
        lines.add("MobsPerSpawn: " + spawner.getMobsPerSpawn());
        lines.add("UseTimer: " + spawner.isUseTimer());
        lines.add("Cooldown: " + spawner.getCooldown() + " | Warmup: " + spawner.getWarmup());
        lines.add("CheckForPlayers: " + spawner.isCheckForPlayers() + " (" + spawner.getActivationRange() + " blocks)");
        lines.add("FieldBoss: " + spawner.isFieldBoss());
        return lines;
    }

    public List<String> getMatchingSpawnerNames(String prefix) {
        String start = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return spawners.keySet().stream()
                .filter(name -> name.startsWith(start))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public boolean moveSpawner(String name, Location location) {
        if (location == null) {
            return false;
        }
        Optional<CustomMobSpawner> spawnerOpt = getSpawner(name);
        if (spawnerOpt.isEmpty()) {
            return false;
        }
        CustomMobSpawner spawner = spawnerOpt.get();
        if (location.getWorld() == null) {
            return false;
        }
        spawner.setWorld(location.getWorld().getName());
        spawner.setX(location.getX());
        spawner.setY(location.getY());
        spawner.setZ(location.getZ());
        save();
        return true;
    }

    public void removeActiveMob(UUID uuid) {
        spawners.values().forEach(spawner -> spawner.getActiveMobs().remove(uuid));
    }
}

package me.nakilex.levelplugin.pet;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.data.PetDataStore;
import me.nakilex.levelplugin.pet.data.PetProfile;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PetManager {
    public static final String PET_TAG = "pet_entity";
    public static final String PET_OWNER_META = "lp_pet_owner";

    private static final int EFFECT_REFRESH_TICKS = 200;
    private static final int EFFECT_DURATION_TICKS = 240;
    private static final double TELEPORT_DISTANCE = 14.0;
    private static final double FOLLOW_DISTANCE = 3.0;

    private final Main plugin;
    private final Map<String, PetDefinition> definitions = new HashMap<>();
    private final Map<UUID, PetInstance> activePets = new HashMap<>();
    private final PetDataStore dataStore;

    public PetManager(Main plugin) {
        this.plugin = plugin;
        this.dataStore = new PetDataStore(plugin);
        loadDefinitions();
    }

    public void loadDefinitions() {
        definitions.clear();
        plugin.saveResource("pets.yml", false);
        File file = new File(plugin.getDataFolder(), "pets.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("pets");
        if (root == null) {
            plugin.getLogger().warning("No pets configured in pets.yml");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection petSection = root.getConfigurationSection(key);
            PetDefinition def = PetDefinition.fromConfig(key, petSection);
            if (def != null) {
                definitions.put(key.toLowerCase(Locale.ROOT), def);
            }
        }
    }

    public void reload() {
        loadDefinitions();
    }

    public List<String> getPetIds() {
        return definitions.values().stream()
                .map(PetDefinition::id)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Optional<PetDefinition> getDefinition(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<PetInstance> getActivePet(UUID ownerId) {
        return Optional.ofNullable(activePets.get(ownerId));
    }

    public void handlePlayerJoin(Player player) {
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        String activeId = profile.activePetId();
        if (activeId != null && !activeId.isBlank()) {
            summonPet(player, activeId);
        }
    }

    public void handlePlayerQuit(Player player) {
        dismissPet(player);
        dataStore.saveProfile(player.getUniqueId());
    }

    public boolean summonPet(Player player, String petId) {
        if (player == null) {
            return false;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            PetChatUtil.send(player, "Unknown pet: " + petId);
            return false;
        }
        dismissPet(player);
        Location spawnLoc = player.getLocation().clone().add(0.6, 0.2, 0.6);
        World world = spawnLoc.getWorld();
        if (world == null) {
            return false;
        }

        ArmorStand stand = world.spawn(spawnLoc, ArmorStand.class, entity -> {
            entity.setCustomNameVisible(true);
            entity.setCustomName(ChatUtil.applyEmojis(def.displayName()));
            entity.setGravity(false);
            entity.setSmall(true);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.addScoreboardTag(PET_TAG);
        });
        stand.setMetadata(PET_OWNER_META, new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        if (!def.modelIds().isEmpty()) {
            ModelEngineUtil.applyModels(stand, def.modelIds(), plugin);
        }

        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        profile.setActivePetId(def.id());
        int xp = profile.getPetXp(def.id());
        int level = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
        PetInstance instance = new PetInstance(player.getUniqueId(), def, stand.getUniqueId(), level, xp);
        activePets.put(player.getUniqueId(), instance);

        applyBonuses(player, instance);
        startFollowTask(player, stand, instance);
        startEffectTask(player, instance);
        PetChatUtil.send(player, "Summoned " + def.displayName() + ".");
        return true;
    }

    public boolean dismissPet(Player player) {
        if (player == null) {
            return false;
        }
        PetInstance instance = activePets.remove(player.getUniqueId());
        if (instance == null) {
            return false;
        }
        removeBonuses(player, instance);
        removeEffects(player, instance);
        instance.cancelTasks();
        Entity entity = Bukkit.getEntity(instance.entityId());
        if (entity != null) {
            entity.remove();
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        profile.setActivePetId(null);
        return true;
    }

    public boolean addPetXp(Player player, String petId, int amount) {
        if (player == null || petId == null) {
            return false;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return false;
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        int currentXp = profile.getPetXp(def.id());
        int newXp = Math.max(0, currentXp + amount);
        profile.setPetXp(def.id(), newXp);

        PetInstance instance = activePets.get(player.getUniqueId());
        if (instance != null && instance.definition().id().equalsIgnoreCase(def.id())) {
            updatePetLevel(player, instance, def, newXp);
        }
        return true;
    }

    public boolean setPetLevel(Player player, String petId, int level) {
        if (player == null || petId == null) {
            return false;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return false;
        }
        int clampedLevel = Math.min(Math.max(1, level), def.maxLevel());
        int xp = PetProgression.xpForLevel(clampedLevel, def.xpPerLevel());
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        profile.setPetXp(def.id(), xp);

        PetInstance instance = activePets.get(player.getUniqueId());
        if (instance != null && instance.definition().id().equalsIgnoreCase(def.id())) {
            updatePetLevel(player, instance, def, xp);
        }
        return true;
    }

    public void shutdown() {
        for (UUID ownerId : Set.copyOf(activePets.keySet())) {
            Player player = Bukkit.getPlayer(ownerId);
            if (player != null) {
                dismissPet(player);
            } else {
                PetInstance instance = activePets.remove(ownerId);
                if (instance != null) {
                    instance.cancelTasks();
                    Entity entity = Bukkit.getEntity(instance.entityId());
                    if (entity != null) {
                        entity.remove();
                    }
                }
            }
        }
        dataStore.saveAll();
    }

    public Map<String, PetDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    private void updatePetLevel(Player player, PetInstance instance, PetDefinition def, int xp) {
        int previousLevel = instance.level();
        int newLevel = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
        instance.setXp(xp);
        if (newLevel != previousLevel) {
            instance.setLevel(newLevel);
            removeBonuses(player, instance);
            applyBonuses(player, instance);
            startEffectTask(player, instance);
            PetChatUtil.send(player, def.displayName() + " reached level " + newLevel + "!");
        }
    }

    private void applyBonuses(Player player, PetInstance instance) {
        Map<StatType, Integer> bonuses = instance.definition().statsForLevel(instance.level());
        instance.setAppliedStats(bonuses);
        if (!bonuses.isEmpty()) {
            StatsManager.getInstance().applyBonusStats(player.getUniqueId(), bonuses);
        }
    }

    private void removeBonuses(Player player, PetInstance instance) {
        Map<StatType, Integer> applied = instance.appliedStats();
        if (!applied.isEmpty()) {
            Map<StatType, Integer> negative = new HashMap<>();
            for (Map.Entry<StatType, Integer> entry : applied.entrySet()) {
                negative.put(entry.getKey(), -entry.getValue());
            }
            StatsManager.getInstance().applyBonusStats(player.getUniqueId(), negative);
        }
        instance.setAppliedStats(Collections.emptyMap());
    }

    private void startFollowTask(Player player, ArmorStand stand, PetInstance instance) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || stand.isDead()) {
                    cancel();
                    return;
                }
                Location ownerLoc = player.getLocation();
                Location petLoc = stand.getLocation();
                double distance = ownerLoc.distance(petLoc);
                if (distance > TELEPORT_DISTANCE) {
                    stand.teleport(ownerLoc.clone().add(0.6, 0.2, 0.6));
                    return;
                }
                if (distance > FOLLOW_DISTANCE) {
                    Vector direction = ownerLoc.toVector().subtract(petLoc.toVector()).normalize();
                    Location target = petLoc.add(direction.multiply(0.4));
                    stand.teleport(target);
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
        instance.setFollowTask(task);
    }

    private void startEffectTask(Player player, PetInstance instance) {
        if (instance.definition().effects().isEmpty()) {
            instance.setAppliedEffects(List.of());
            return;
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                applyEffects(player, instance);
            }
        }.runTaskTimer(plugin, 0L, EFFECT_REFRESH_TICKS);
        instance.setEffectTask(task);
    }

    private void applyEffects(Player player, PetInstance instance) {
        List<PetEffectDefinition> scaled = instance.definition().effectsForLevel(instance.level());
        instance.setAppliedEffects(scaled);
        for (PetEffectDefinition effect : scaled) {
            PotionEffectUtil.applyHiddenEffect(player, effect.type(), EFFECT_DURATION_TICKS, effect.baseAmplifier());
        }
    }

    private void removeEffects(Player player, PetInstance instance) {
        for (PetEffectDefinition effect : instance.appliedEffects()) {
            PotionEffectUtil.removeEffect(player, effect.type());
        }
        instance.setAppliedEffects(List.of());
    }

    public PetProfile getProfile(UUID uuid) {
        return dataStore.getProfile(uuid);
    }

    public Map<StatType, Integer> getPetStats(String petId, int level) {
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return Map.of();
        }
        return def.statsForLevel(level);
    }

    public ItemRarity getPetRarity(String petId) {
        PetDefinition def = getDefinition(petId).orElse(null);
        return def == null ? ItemRarity.COMMON : def.rarity();
    }
}

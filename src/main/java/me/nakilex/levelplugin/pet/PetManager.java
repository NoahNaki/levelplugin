package me.nakilex.levelplugin.pet;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.data.PetDataStore;
import me.nakilex.levelplugin.pet.data.PetProfile;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetDisplayUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PetManager {
    public static final String PET_TAG = "pet_entity";
    public static final String PET_OWNER_META = "lp_pet_owner";

    private static final double TELEPORT_DISTANCE = 32.0;
    private static final double FOLLOW_DISTANCE = 4.0;
    private static final int MAX_TIER = 5;
    private static final List<ItemRarity> GACHA_RARITIES = List.of(
            ItemRarity.COMMON,
            ItemRarity.UNCOMMON,
            ItemRarity.RARE,
            ItemRarity.EPIC,
            ItemRarity.LEGENDARY,
            ItemRarity.MYTHIC
    );
    private static final Map<ItemRarity, Double> GACHA_WEIGHTS = Map.of(
            ItemRarity.COMMON, 55.0,
            ItemRarity.UNCOMMON, 25.0,
            ItemRarity.RARE, 12.0,
            ItemRarity.EPIC, 6.0,
            ItemRarity.LEGENDARY, 1.5,
            ItemRarity.MYTHIC, 0.5
    );
    private static final Map<ItemRarity, Integer> SELL_VALUES = Map.of(
            ItemRarity.COMMON, 25,
            ItemRarity.UNCOMMON, 50,
            ItemRarity.RARE, 100,
            ItemRarity.EPIC, 200,
            ItemRarity.LEGENDARY, 500,
            ItemRarity.MYTHIC, 1000
    );

    private final Main plugin;
    private final Map<String, PetDefinition> definitions = new HashMap<>();
    private final Map<UUID, PetInstance> activePets = new HashMap<>();
    private final PetDataStore dataStore;
    private final Map<UUID, Long> lastMoveAt = new HashMap<>();
    private final Map<UUID, Long> lastStandCooldownAt = new HashMap<>();
    private final Map<UUID, Long> lastStandImmuneUntil = new HashMap<>();
    private final Map<UUID, Long> lastStandBuffUntil = new HashMap<>();
    private final Map<UUID, Map<StatType, Integer>> appliedOwnershipStats = new HashMap<>();

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

    public List<PetDefinition> getOwnedPets(UUID ownerId) {
        PetProfile profile = dataStore.getProfile(ownerId);
        return definitions.values().stream()
                .filter(def -> profile.getPetCopies(def.id()) > 0)
                .sorted(Comparator.comparing((PetDefinition def) -> def.rarity().ordinal())
                        .thenComparing(PetDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ItemRarity> getGachaRarities() {
        return GACHA_RARITIES;
    }

    public int getMaxTier() {
        return MAX_TIER;
    }

    public int getSellValue(ItemRarity rarity) {
        return SELL_VALUES.getOrDefault(rarity, 25);
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
        recordMovement(player.getUniqueId());
        refreshOwnershipBonuses(player.getUniqueId());
        if (activeId != null && !activeId.isBlank()) {
            summonPet(player, activeId);
        }
    }

    public void handlePlayerQuit(Player player) {
        dismissPet(player);
        dataStore.saveProfile(player.getUniqueId());
        clearPlayerState(player.getUniqueId());
    }

    public void handleProfileDeletion(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
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
        removePetEntities(ownerId);
        clearOwnershipBonuses(ownerId);
        dataStore.clearProfile(ownerId);
        clearPlayerState(ownerId);
    }

    public void recordMovement(UUID playerId) {
        if (playerId == null) {
            return;
        }
        lastMoveAt.put(playerId, System.currentTimeMillis());
    }

    public boolean isStationary(UUID playerId, long minStillMs) {
        if (playerId == null) {
            return false;
        }
        long lastMove = lastMoveAt.getOrDefault(playerId, 0L);
        return System.currentTimeMillis() - lastMove >= minStillMs;
    }

    public boolean isLastStandImmune(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return System.currentTimeMillis() < lastStandImmuneUntil.getOrDefault(playerId, 0L);
    }

    public double getLastStandDamageBoost(UUID playerId) {
        if (playerId == null) {
            return 0.0;
        }
        return System.currentTimeMillis() < lastStandBuffUntil.getOrDefault(playerId, 0L) ? 1.5 : 0.0;
    }

    public boolean tryActivateLastStand(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now < lastStandCooldownAt.getOrDefault(playerId, 0L)) {
            return false;
        }
        long immuneUntil = now + 5_000L;
        lastStandImmuneUntil.put(playerId, immuneUntil);
        lastStandBuffUntil.put(playerId, immuneUntil);
        lastStandCooldownAt.put(playerId, now + 600_000L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            double maxHealth = player.getMaxHealth();
            double healed = Math.min(maxHealth, player.getHealth() + maxHealth * 0.10);
            player.setHealth(healed);
        }, 100L);
        return true;
    }

    private void clearPlayerState(UUID playerId) {
        lastMoveAt.remove(playerId);
        lastStandCooldownAt.remove(playerId);
        lastStandImmuneUntil.remove(playerId);
        lastStandBuffUntil.remove(playerId);
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
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        if (profile.getPetCopies(def.id()) <= 0) {
            PetChatUtil.send(player, "You do not own that pet yet.");
            if (def.id().equalsIgnoreCase(profile.activePetId())) {
                profile.setActivePetId(null);
            }
            return false;
        }
        PetInstance active = activePets.get(player.getUniqueId());
        if (active != null && active.definition().id().equalsIgnoreCase(def.id())) {
            PetChatUtil.send(player, "That pet is already summoned.");
            return false;
        }
        dismissPet(player);
        removePetEntities(player.getUniqueId());
        Location spawnLoc = player.getLocation().clone().add(0.6, 0.2, 0.6);
        World world = spawnLoc.getWorld();
        if (world == null) {
            return false;
        }

        String displayName = PetDisplayUtil.formatDisplayName(def);
        ArmorStand stand = world.spawn(spawnLoc, ArmorStand.class, entity -> {
            entity.setCustomNameVisible(true);
            entity.setCustomName(displayName);
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

        profile.setActivePetId(def.id());
        int xp = profile.getPetXp(def.id());
        int tier = profile.getPetTier(def.id());
        int level = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
        PetInstance instance = new PetInstance(player.getUniqueId(), def, stand.getUniqueId(), level, xp, tier);
        activePets.put(player.getUniqueId(), instance);

        applyBonuses(player, instance);
        startFollowTask(player, stand, instance);
        startEffectTask(instance);
        PetChatUtil.send(player, "Summoned " + displayName + ".");
        return true;
    }

    public boolean dismissPet(Player player) {
        if (player == null) {
            return false;
        }
        PetInstance instance = activePets.remove(player.getUniqueId());
        if (instance == null) {
            removePetEntities(player.getUniqueId());
            return false;
        }
        removeBonuses(player, instance);
        removeEffects(instance);
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

    public void addActivePetXp(UUID ownerId, int amount) {
        if (ownerId == null || amount <= 0) {
            return;
        }
        PetInstance instance = activePets.get(ownerId);
        if (instance == null) {
            return;
        }
        addPetXp(Bukkit.getPlayer(ownerId), instance.definition().id(), amount);
    }

    public boolean investTier(Player player, String petId) {
        if (player == null || petId == null) {
            return false;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return false;
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        int tier = profile.getPetTier(def.id());
        if (tier >= MAX_TIER) {
            return false;
        }
        int copies = profile.getPetCopies(def.id());
        int available = getInvestableCopies(profile, def);
        if (available <= 0) {
            return false;
        }
        profile.setPetCopies(def.id(), Math.max(1, copies - 1));
        profile.setPetTier(def.id(), tier + 1);
        refreshOwnershipBonuses(player.getUniqueId());
        PetInstance instance = activePets.get(player.getUniqueId());
        if (instance != null && instance.definition().id().equalsIgnoreCase(def.id())) {
            instance.setTier(tier + 1);
            removeBonuses(player, instance);
            applyBonuses(player, instance);
            startEffectTask(instance);
        }
        return true;
    }

    public int getInvestableCopies(Player player, String petId) {
        if (player == null || petId == null) {
            return 0;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return 0;
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        return getInvestableCopies(profile, def);
    }

    public int getSellableCopies(Player player, String petId) {
        if (player == null || petId == null) {
            return 0;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return 0;
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        return getSellableCopies(profile, def);
    }

    public int sellPetCopies(Player player, String petId, int amount) {
        if (player == null || petId == null || amount <= 0) {
            return 0;
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return 0;
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        int sellable = getSellableCopies(profile, def);
        int actual = Math.min(amount, sellable);
        if (actual <= 0) {
            return 0;
        }
        int current = profile.getPetCopies(def.id());
        profile.setPetCopies(def.id(), Math.max(0, current - actual));
        refreshOwnershipBonuses(player.getUniqueId());
        int perCopy = SELL_VALUES.getOrDefault(def.rarity(), 25);
        int total = perCopy * actual;
        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().addCoins(player, total);
        }
        return total;
    }

    public InvestResult investAllCopies(Player player, String petId) {
        if (player == null || petId == null) {
            return new InvestResult(0, 0, 0);
        }
        PetDefinition def = getDefinition(petId).orElse(null);
        if (def == null) {
            return new InvestResult(0, 0, 0);
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        int tier = profile.getPetTier(def.id());
        int copies = profile.getPetCopies(def.id());
        int available = Math.max(0, copies - 1);
        int maxTier = MAX_TIER;
        int investable = Math.min(available, Math.max(0, maxTier - tier));
        if (investable <= 0 && tier < maxTier) {
            return new InvestResult(0, 0, 0);
        }
        int newTier = tier + investable;
        int remainingCopies = copies - investable;
        int soldCopies = 0;
        int coins = 0;
        if (newTier >= maxTier && remainingCopies > 1) {
            soldCopies = remainingCopies - 1;
            remainingCopies = 1;
            coins = soldCopies * SELL_VALUES.getOrDefault(def.rarity(), 25);
            if (plugin.getEconomyManager() != null && coins > 0) {
                plugin.getEconomyManager().addCoins(player, coins);
            }
        }
        if (investable > 0) {
            profile.setPetTier(def.id(), newTier);
            profile.setPetCopies(def.id(), Math.max(1, remainingCopies));
            refreshOwnershipBonuses(player.getUniqueId());
            PetInstance instance = activePets.get(player.getUniqueId());
            if (instance != null && instance.definition().id().equalsIgnoreCase(def.id())) {
                instance.setTier(newTier);
                removeBonuses(player, instance);
                applyBonuses(player, instance);
                startEffectTask(instance);
            }
        } else {
            profile.setPetCopies(def.id(), Math.max(1, remainingCopies));
            refreshOwnershipBonuses(player.getUniqueId());
        }
        return new InvestResult(investable, soldCopies, coins);
    }

    public void addPetCopies(Player player, String petId, int amount) {
        if (player == null || petId == null || amount <= 0) {
            return;
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        profile.addPetCopies(petId, amount);
        refreshOwnershipBonuses(player.getUniqueId());
    }

    public PetPullResult pullPets(Player player, int amount) {
        PetPullDetailed detailed = pullPetsDetailed(player, amount);
        return new PetPullResult(detailed.kept(), detailed.discarded());
    }

    public PetPullDetailed pullPetsDetailed(Player player, int amount) {
        if (player == null || amount <= 0) {
            return new PetPullDetailed(List.of(), Map.of(), Map.of());
        }
        Map<ItemRarity, List<PetDefinition>> pools = buildRarityPools();
        if (pools.isEmpty()) {
            return new PetPullDetailed(List.of(), Map.of(), Map.of());
        }
        PetProfile profile = dataStore.getProfile(player.getUniqueId());
        List<PetPullEntry> pulls = new ArrayList<>(amount);
        Map<PetDefinition, Integer> kept = new HashMap<>();
        Map<PetDefinition, Integer> discarded = new HashMap<>();
        Random random = ThreadLocalRandom.current();
        Map<ItemRarity, Double> weights = buildRarityWeights(pools);
        for (int i = 0; i < amount; i++) {
            ItemRarity rarity = me.nakilex.levelplugin.utils.RandomUtil.pickWeighted(random, weights);
            List<PetDefinition> options = pools.get(rarity);
            if (options == null || options.isEmpty()) {
                continue;
            }
            PetDefinition def = options.get(random.nextInt(options.size()));
            if (shouldDiscard(def, profile.autoDiscardRarity())) {
                discarded.merge(def, 1, Integer::sum);
                pulls.add(new PetPullEntry(def, false));
                continue;
            }
            if (profile.getPetCopies(def.id()) <= 0) {
                profile.setPetTier(def.id(), 1);
            }
            profile.addPetCopies(def.id(), 1);
            kept.merge(def, 1, Integer::sum);
            pulls.add(new PetPullEntry(def, true));
        }
        refreshOwnershipBonuses(player.getUniqueId());
        return new PetPullDetailed(pulls, kept, discarded);
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
        for (UUID ownerId : Set.copyOf(appliedOwnershipStats.keySet())) {
            clearOwnershipBonuses(ownerId);
        }
        dataStore.saveAll();
    }

    public Map<StatType, Integer> getTotalOwnedStatBonuses(UUID playerId) {
        Map<StatType, Integer> totals = calculateOwnershipBonuses(playerId);
        return totals.isEmpty() ? Map.of() : Collections.unmodifiableMap(totals);
    }

    public void refreshOwnershipBonuses(UUID playerId) {
        if (playerId == null) {
            return;
        }
        Map<StatType, Integer> previous = appliedOwnershipStats.getOrDefault(playerId, Map.of());
        Map<StatType, Integer> current = calculateOwnershipBonuses(playerId);
        Map<StatType, Integer> delta = subtractStats(current, previous);
        if (!delta.isEmpty()) {
            StatsManager.getInstance().applyBonusStats(playerId, delta);
        }
        if (current.isEmpty()) {
            appliedOwnershipStats.remove(playerId);
        } else {
            appliedOwnershipStats.put(playerId, current);
        }
    }

    private void clearOwnershipBonuses(UUID playerId) {
        Map<StatType, Integer> applied = appliedOwnershipStats.remove(playerId);
        if (applied == null || applied.isEmpty()) {
            return;
        }
        Map<StatType, Integer> negative = new EnumMap<>(StatType.class);
        for (Map.Entry<StatType, Integer> entry : applied.entrySet()) {
            if (entry.getValue() != null && entry.getValue() != 0) {
                negative.put(entry.getKey(), -entry.getValue());
            }
        }
        if (!negative.isEmpty()) {
            StatsManager.getInstance().applyBonusStats(playerId, negative);
        }
    }

    private Map<StatType, Integer> calculateOwnershipBonuses(UUID playerId) {
        if (playerId == null) {
            return Map.of();
        }
        PetProfile profile = dataStore.getProfile(playerId);
        Map<StatType, Integer> totals = new EnumMap<>(StatType.class);
        for (PetDefinition definition : definitions.values()) {
            if (profile.getPetCopies(definition.id()) <= 0) {
                continue;
            }
            mergeStats(totals, definition.ownershipStats());
        }
        return totals;
    }

    private static void mergeStats(Map<StatType, Integer> target, Map<StatType, Integer> additions) {
        if (target == null || additions == null || additions.isEmpty()) {
            return;
        }
        for (Map.Entry<StatType, Integer> entry : additions.entrySet()) {
            StatType type = entry.getKey();
            int value = entry.getValue() == null ? 0 : entry.getValue();
            if (type == null || value == 0) {
                continue;
            }
            target.merge(type, value, Integer::sum);
        }
    }

    private static Map<StatType, Integer> subtractStats(Map<StatType, Integer> lhs, Map<StatType, Integer> rhs) {
        Map<StatType, Integer> delta = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            int left = lhs == null ? 0 : lhs.getOrDefault(type, 0);
            int right = rhs == null ? 0 : rhs.getOrDefault(type, 0);
            int diff = left - right;
            if (diff != 0) {
                delta.put(type, diff);
            }
        }
        return delta;
    }

    public Map<String, PetDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    private Map<ItemRarity, List<PetDefinition>> buildRarityPools() {
        Map<ItemRarity, List<PetDefinition>> pools = new EnumMap<>(ItemRarity.class);
        for (PetDefinition def : definitions.values()) {
            if (!GACHA_RARITIES.contains(def.rarity())) {
                continue;
            }
            pools.computeIfAbsent(def.rarity(), rarity -> new java.util.ArrayList<>()).add(def);
        }
        return pools;
    }

    private Map<ItemRarity, Double> buildRarityWeights(Map<ItemRarity, List<PetDefinition>> pools) {
        Map<ItemRarity, Double> weights = new EnumMap<>(ItemRarity.class);
        for (Map.Entry<ItemRarity, List<PetDefinition>> entry : pools.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            weights.put(entry.getKey(), GACHA_WEIGHTS.getOrDefault(entry.getKey(), 1.0));
        }
        return weights;
    }

    private boolean shouldDiscard(PetDefinition def, ItemRarity autoDiscardRarity) {
        if (def == null || autoDiscardRarity == null) {
            return false;
        }
        return def.rarity().ordinal() <= autoDiscardRarity.ordinal();
    }

    private void removePetEntities(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        String ownerToken = ownerId.toString();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.hasMetadata(PET_OWNER_META)) {
                    continue;
                }
                for (var value : entity.getMetadata(PET_OWNER_META)) {
                    if (ownerToken.equalsIgnoreCase(value.asString())) {
                        entity.remove();
                        break;
                    }
                }
            }
        }
    }

    private int getInvestableCopies(PetProfile profile, PetDefinition def) {
        int copies = profile.getPetCopies(def.id());
        return Math.max(0, copies - 1);
    }

    private int getSellableCopies(PetProfile profile, PetDefinition def) {
        int copies = profile.getPetCopies(def.id());
        return Math.max(0, copies - 1);
    }

    private void updatePetLevel(Player player, PetInstance instance, PetDefinition def, int xp) {
        int previousLevel = instance.level();
        int newLevel = PetProgression.levelFromXp(xp, def.xpPerLevel(), def.maxLevel());
        instance.setXp(xp);
        if (newLevel != previousLevel) {
            instance.setLevel(newLevel);
            removeBonuses(player, instance);
            applyBonuses(player, instance);
            startEffectTask(instance);
            PetChatUtil.send(player, def.displayName() + " reached level " + newLevel + "!");
        }
    }

    private void applyBonuses(Player player, PetInstance instance) {
        instance.setAppliedStats(Collections.emptyMap());
    }

    private void removeBonuses(Player player, PetInstance instance) {
        instance.setAppliedStats(Collections.emptyMap());
    }

    private void startFollowTask(Player player, ArmorStand stand, PetInstance instance) {
        BukkitTask task = new BukkitRunnable() {
            private double ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || stand.isDead()) {
                    cancel();
                    return;
                }
                ticks += 10;
                Location ownerLoc = player.getLocation();
                Location petLoc = stand.getLocation();
                double bob = Math.sin(ticks / 20.0) * 0.12;
                Location desired = ownerLoc.clone().add(0.8, 0.4 + bob, 0.8);
                double distance = ownerLoc.distance(petLoc);
                if (distance > TELEPORT_DISTANCE) {
                    stand.teleport(desired);
                    return;
                }
                if (distance > FOLLOW_DISTANCE) {
                    Vector direction = desired.toVector().subtract(petLoc.toVector()).normalize();
                    Location target = petLoc.add(direction.multiply(1.2));
                    target.setY(desired.getY());
                    stand.teleport(target);
                } else {
                    Location hover = petLoc.clone();
                    hover.setY(desired.getY());
                    stand.teleport(hover);
                }
            }
        }.runTaskTimer(plugin, 5L, 3L);
        instance.setFollowTask(task);
    }

    private void startEffectTask(PetInstance instance) {
        instance.setEffectTask(null);
        applyEffects(instance);
    }

    private void applyEffects(PetInstance instance) {
        List<PetEffectDefinition> scaled = instance.definition().effectsForLevel(instance.level(), instance.tier());
        instance.setAppliedEffects(scaled);
    }

    private void removeEffects(PetInstance instance) {
        instance.setAppliedEffects(List.of());
    }

    public List<PetEffectDefinition> getActiveEffects(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        PetInstance instance = activePets.get(playerId);
        if (instance == null) {
            return List.of();
        }
        return instance.appliedEffects();
    }

    public double getActiveEffectValue(UUID playerId, PetEffectType type) {
        if (playerId == null || type == null) {
            return 0.0;
        }
        double total = 0.0;
        for (PetEffectDefinition effect : getActiveEffects(playerId)) {
            if (type == effect.type()) {
                total += effect.baseValue();
            }
        }
        return total;
    }

    public PetProfile getProfile(UUID uuid) {
        return dataStore.getProfile(uuid);
    }

    public Location getPendingSummonReturn(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return dataStore.getProfile(playerId).pendingSummonReturn();
    }

    public void setPendingSummonReturn(UUID playerId, Location location) {
        if (playerId == null) {
            return;
        }
        PetProfile profile = dataStore.getProfile(playerId);
        profile.setPendingSummonReturn(location);
        dataStore.saveProfile(playerId);
    }

    public void clearPendingSummonReturn(UUID playerId) {
        if (playerId == null) {
            return;
        }
        PetProfile profile = dataStore.getProfile(playerId);
        profile.clearPendingSummonReturn();
        dataStore.saveProfile(playerId);
    }

    public ItemRarity getPetRarity(String petId) {
        PetDefinition def = getDefinition(petId).orElse(null);
        return def == null ? ItemRarity.COMMON : def.rarity();
    }

    public record InvestResult(int investedCopies, int soldCopies, int coinsEarned) {}

    public record PetPullEntry(PetDefinition definition, boolean kept) {}

    public record PetPullDetailed(List<PetPullEntry> pulls,
                                  Map<PetDefinition, Integer> kept,
                                  Map<PetDefinition, Integer> discarded) {}

    public record PetPullResult(Map<PetDefinition, Integer> kept, Map<PetDefinition, Integer> discarded) {}
}

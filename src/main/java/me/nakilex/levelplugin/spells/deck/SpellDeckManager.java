package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.RandomUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class SpellDeckManager {
    private static final SpellDeckManager INSTANCE = new SpellDeckManager();
    private static final int PITY_THRESHOLD = 60;
    private static final List<SpellDeckRarity> GACHA_RARITIES = List.of(
            SpellDeckRarity.COMMON,
            SpellDeckRarity.UNCOMMON,
            SpellDeckRarity.RARE,
            SpellDeckRarity.EPIC,
            SpellDeckRarity.LEGENDARY,
            SpellDeckRarity.MYTHIC
    );
    private static final int MAX_MASTERY_RANK = 5;
    private static final double MASTERY_MANA_COOLDOWN_REDUCTION_PER_RANK = 0.02;
    private static final Map<SpellDeckRarity, Double> GACHA_WEIGHTS = Map.of(
            SpellDeckRarity.COMMON, 55.0,
            SpellDeckRarity.UNCOMMON, 25.0,
            SpellDeckRarity.RARE, 12.0,
            SpellDeckRarity.EPIC, 6.0,
            SpellDeckRarity.LEGENDARY, 1.5,
            SpellDeckRarity.MYTHIC, 0.5
    );
    private static final Map<SpellDeckRarity, Integer> MAXED_DUPLICATE_GEMS = Map.of(
            SpellDeckRarity.COMMON, 2,
            SpellDeckRarity.UNCOMMON, 5,
            SpellDeckRarity.RARE, 10,
            SpellDeckRarity.EPIC, 25,
            SpellDeckRarity.LEGENDARY, 75,
            SpellDeckRarity.MYTHIC, 150
    );

    public static SpellDeckManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, SpellCardDefinition> definitions = new HashMap<>();
    private final Map<String, SpellCardDefinition> definitionsBySpellId = new HashMap<>();
    private final Map<String, List<SpellCardDefinition>> definitionsByFamily = new HashMap<>();
    private SpellDeckDataStore dataStore;
    private Main plugin;

    private SpellDeckManager() {
    }

    public void init(Main plugin) {
        this.plugin = plugin;
        this.dataStore = new SpellDeckDataStore(plugin);
        registerDefaults();
    }

    public void shutdown() {
        if (dataStore != null) {
            dataStore.saveAll();
        }
    }

    public void registerDefaults() {
        definitions.clear();
        definitionsBySpellId.clear();
        definitionsByFamily.clear();
        register(new SpellCardDefinition("fireball_common", "fireball", "deck_fireball_common", "Fireball",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Shoots a fireball forward.", "Damage: 100", "Mana Cost: 20", "Cooldown: 6s", "Explosion Radius: 2.5 blocks", "Burn: 3s at 10/sec", "Projectile Speed: 1.2 blocks/tick"),
                List.of()));
        register(new SpellCardDefinition("fireball_uncommon", "fireball", "deck_fireball_uncommon", "Enhanced Fireball",
                SpellDeckRarity.UNCOMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Damage: 120", "Explosion Radius: 3.5 blocks", "Leaves burning ground for 4s", "Ground burn: 15/sec in 3 blocks"),
                List.of("Cooldown increased to 7s")));
        register(new SpellCardDefinition("fireball_rare", "fireball", "deck_fireball_rare", "Infernal Fireball",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Damage: 150", "Burn: 5s at 16/sec", "Burning enemies spread flames every second", "Spread: 2 blocks, 3s at 8/sec", "Max chain depth: 2"),
                List.of("Explosion Radius reduced to 3 blocks")));
        register(new SpellCardDefinition("fireball_epic", "fireball", "deck_fireball_epic", "Cataclysm Fireball",
                SpellDeckRarity.EPIC, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Initial Damage: 180", "Explosion Radius: 4 blocks", "Creates 3 delayed secondary explosions", "Secondary: 90 damage in 2.5 blocks", "Burn: 6s at 18/sec"),
                List.of("Mana Cost increased to 35", "Cooldown increased to 9s")));
        register(new SpellCardDefinition("fireball_legendary", "fireball", "deck_fireball_legendary", "Dragonfire Orb",
                SpellDeckRarity.LEGENDARY, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Charge by holding right-click up to 2.5s", "Base: 170 damage in 3.5 blocks", "Full charge: 350 damage in 7 blocks", "Full charge burn: 8s at 24/sec", "Full charge creates 5 secondary explosions", "Full charge briefly stuns enemies"),
                List.of("Mana scales from 30 to 60", "Movement is reduced while charging")));
        register(new SpellCardDefinition("fireball_mythic", "fireball", "deck_fireball_mythic", "Worldfire",
                SpellDeckRarity.MYTHIC, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Initial Hit: 250 damage in 5 blocks", "Burn: 10s at 30/sec", "Burning deaths trigger Living Inferno", "Chain explosion: 140 damage in 4 blocks", "Chains up to 10 times at -15% damage", "Burning enemies reduce nearby fire resistance"),
                List.of("Mana Cost: 75", "Cooldown: 14s", "Below 20% HP: lose 10% current HP")));
    }

    public void register(SpellCardDefinition definition) {
        if (definition == null) {
            return;
        }
        String cardId = normalize(definition.cardId());
        definitions.put(cardId, definition);
        definitionsBySpellId.put(normalize(definition.spellId()), definition);
        definitionsByFamily.computeIfAbsent(normalize(definition.familyId()), ignored -> new ArrayList<>()).add(definition);
        definitionsByFamily.values().forEach(list -> list.sort(java.util.Comparator.comparingInt(card -> card.rarity().ordinal())));
    }

    public Collection<SpellCardDefinition> getDefinitions() {
        return List.copyOf(definitions.values());
    }

    public List<SpellCardDefinition> getDefinitionsForFamily(String familyId) {
        return List.copyOf(definitionsByFamily.getOrDefault(normalize(familyId), List.of()));
    }

    public List<SpellCardDefinition> getOwnedCards(UUID playerId) {
        if (dataStore == null || playerId == null) {
            return List.of();
        }
        SpellDeckProfile profile = dataStore.getProfile(playerId);
        List<SpellCardDefinition> owned = new ArrayList<>();
        for (SpellCardDefinition definition : definitions.values()) {
            if (profile.getCopies(definition.cardId()) > 0) {
                owned.add(definition);
            }
        }
        owned.sort(java.util.Comparator.comparing((SpellCardDefinition card) -> card.familyId().toLowerCase(Locale.ROOT))
                .thenComparingInt(card -> card.rarity().ordinal()));
        return owned;
    }

    public SpellDeckProfile getProfile(UUID playerId) {
        if (dataStore == null || playerId == null) {
            return null;
        }
        return dataStore.getProfile(playerId);
    }

    public SpellCardDefinition getDefinition(String cardId) {
        return definitions.get(normalize(cardId));
    }

    public SpellCardDefinition getDefinitionBySpellId(String spellId) {
        return definitionsBySpellId.get(normalize(spellId));
    }

    public SpellCardDefinition getEquippedCard(UUID playerId, SpellInputType inputType) {
        if (dataStore == null || playerId == null || inputType == null) {
            return null;
        }
        return getDefinition(dataStore.getProfile(playerId).getEquippedCardId(inputType));
    }

    public SpellRegistry.SpellEntry getEquippedSpellEntry(Player player, SpellInputType inputType) {
        if (player == null) {
            return null;
        }
        SpellCardDefinition card = getEquippedCard(player.getUniqueId(), inputType);
        if (card == null) {
            return null;
        }
        return SpellRegistry.getInstance().getSpell(card.spellId());
    }

    public boolean hasEquippedCard(Player player, SpellInputType inputType) {
        return getEquippedCard(player == null ? null : player.getUniqueId(), inputType) != null;
    }

    public boolean hasAnyEquippedCard(Player player) {
        if (player == null || dataStore == null) {
            return false;
        }
        return !dataStore.getProfile(player.getUniqueId()).equippedCards().isEmpty();
    }

    public int getCopies(UUID playerId, String cardId) {
        if (dataStore == null || playerId == null) {
            return 0;
        }
        return dataStore.getProfile(playerId).getCopies(cardId);
    }

    public boolean equip(Player player, SpellInputType inputType, String cardId) {
        if (player == null || inputType == null || cardId == null || dataStore == null) {
            return false;
        }
        SpellCardDefinition definition = getDefinition(cardId);
        if (definition == null) {
            return false;
        }
        SpellDeckProfile profile = dataStore.getProfile(player.getUniqueId());
        if (profile.getCopies(definition.cardId()) <= 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You have not pulled " + definition.displayName() + " yet.");
            return false;
        }
        SpellInputType existingSlot = getEquippedSlotForFamily(profile, definition.familyId());
        if (existingSlot != null && existingSlot != inputType) {
            SpellCardDefinition existingCard = getDefinition(profile.getEquippedCardId(existingSlot));
            String spellName = existingCard == null ? definition.displayName() : existingCard.displayName();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    spellName + " is already equipped in " + labelForInput(existingSlot) + ".");
            return false;
        }
        removeEquippedFamilyCopies(profile, definition.familyId(), inputType);
        profile.equip(inputType, definition.cardId());
        dataStore.saveProfile(player.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Equipped " + definition.rarity().color() + definition.displayName() + org.bukkit.ChatColor.GREEN + " to " + inputType.name().replace('_', ' ') + ".");
        return true;
    }


    public SpellInputType getEquippedSlotForFamily(SpellDeckProfile profile, String familyId) {
        if (profile == null || familyId == null || familyId.isBlank()) {
            return null;
        }
        String normalizedFamily = normalize(familyId);
        for (Map.Entry<SpellInputType, String> entry : profile.equippedCards().entrySet()) {
            SpellCardDefinition equipped = getDefinition(entry.getValue());
            if (equipped != null && normalize(equipped.familyId()).equals(normalizedFamily)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void removeEquippedFamilyCopies(SpellDeckProfile profile, String familyId, SpellInputType exceptSlot) {
        if (profile == null || familyId == null || familyId.isBlank()) {
            return;
        }
        String normalizedFamily = normalize(familyId);
        for (SpellInputType slot : List.copyOf(profile.equippedCards().keySet())) {
            if (slot == exceptSlot) {
                continue;
            }
            SpellCardDefinition equipped = getDefinition(profile.getEquippedCardId(slot));
            if (equipped != null && normalize(equipped.familyId()).equals(normalizedFamily)) {
                profile.equip(slot, null);
            }
        }
    }

    private String labelForInput(SpellInputType inputType) {
        if (inputType == null) {
            return "another slot";
        }
        return switch (inputType) {
            case SPELL_1 -> "Spell 1";
            case SPELL_2 -> "Spell 2";
            case SPELL_3 -> "Spell 3";
            case SPELL_4 -> "Spell 4";
            case BASIC_ATTACK -> "Basic Attack";
        };
    }

    public SpellPullResult pull(Player player, int amount) {
        if (player == null || amount <= 0 || dataStore == null || definitions.isEmpty()) {
            return new SpellPullResult(List.of(), Map.of(), 0, 0);
        }
        SpellDeckProfile profile = dataStore.getProfile(player.getUniqueId());
        Map<SpellDeckRarity, List<SpellCardDefinition>> pools = buildRarityPools();
        Map<SpellDeckRarity, Double> weights = buildRarityWeights(pools);
        List<SpellPullEntry> entries = new ArrayList<>(amount);
        Map<SpellCardDefinition, Integer> summary = new HashMap<>();
        int duplicateInvestments = 0;
        int salvagedGems = 0;
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            boolean pityGuaranteed = profile.pityPullsSinceLegendary() >= (PITY_THRESHOLD - 1);
            SpellCardDefinition card = rollCard(random, pools, weights, pityGuaranteed);
            if (card == null) {
                continue;
            }
            if (card.rarity().ordinal() >= SpellDeckRarity.LEGENDARY.ordinal()) {
                profile.setPityPullsSinceLegendary(0);
            } else {
                profile.setPityPullsSinceLegendary(profile.pityPullsSinceLegendary() + 1);
            }
            int existingCopies = profile.getCopies(card.cardId());
            int invested = profile.getInvestedCopies(card.cardId());
            if (existingCopies <= 0) {
                profile.addCopies(card.cardId(), 1);
                autoEquipFirstCopy(player, profile, card);
                entries.add(new SpellPullEntry(card));
            } else if (invested < maxMasteryInvestedCopies()) {
                profile.addInvestedCopies(card.cardId(), 1);
                duplicateInvestments++;
                entries.add(new SpellPullEntry(card, true, 0));
            } else {
                int gems = maxedDuplicateGemValue(card.rarity());
                addGems(player, gems);
                salvagedGems += gems;
                entries.add(new SpellPullEntry(card, false, gems));
            }
            summary.merge(card, 1, Integer::sum);
        }
        dataStore.saveProfile(player.getUniqueId());
        return new SpellPullResult(entries, summary, duplicateInvestments, salvagedGems);
    }

    public List<SpellDeckRarity> getGachaRarities() {
        return GACHA_RARITIES;
    }

    public Map<SpellDeckRarity, Double> getGachaRates() {
        return Collections.unmodifiableMap(GACHA_WEIGHTS);
    }

    public int getPityThreshold() {
        return PITY_THRESHOLD;
    }

    public int getPityPullsSinceLegendary(UUID playerId) {
        if (dataStore == null || playerId == null) {
            return 0;
        }
        return dataStore.getProfile(playerId).pityPullsSinceLegendary();
    }

    public InvestAllResult investAllDuplicateCopies(Player player) {
        if (player == null || dataStore == null) {
            return new InvestAllResult(0, 0, 0);
        }
        SpellDeckProfile profile = dataStore.getProfile(player.getUniqueId());
        int cardsTouched = 0;
        int copiesInvested = 0;
        int gemsSalvaged = 0;
        for (SpellCardDefinition definition : definitions.values()) {
            int copies = profile.getCopies(definition.cardId());
            if (copies <= 1) {
                continue;
            }
            int duplicateCopies = copies - 1;
            int availableMastery = Math.max(0, maxMasteryInvestedCopies() - profile.getInvestedCopies(definition.cardId()));
            int investableCopies = Math.min(duplicateCopies, availableMastery);
            int salvageCopies = duplicateCopies - investableCopies;
            profile.setCopies(definition.cardId(), 1);
            if (investableCopies > 0) {
                profile.addInvestedCopies(definition.cardId(), investableCopies);
                copiesInvested += investableCopies;
            }
            if (salvageCopies > 0) {
                gemsSalvaged += salvageCopies * maxedDuplicateGemValue(definition.rarity());
            }
            cardsTouched++;
        }
        if (gemsSalvaged > 0) {
            addGems(player, gemsSalvaged);
        }
        if (copiesInvested > 0 || gemsSalvaged > 0) {
            dataStore.saveProfile(player.getUniqueId());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Invested " + org.bukkit.ChatColor.WHITE + copiesInvested + org.bukkit.ChatColor.GREEN
                            + " duplicate spell copies and salvaged " + org.bukkit.ChatColor.LIGHT_PURPLE
                            + gemsSalvaged + " <glyph:purple_orb_icon>" + org.bukkit.ChatColor.GREEN + ".");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You do not have duplicate spell cards to invest.");
        }
        return new InvestAllResult(cardsTouched, copiesInvested, gemsSalvaged);
    }

    public int getMaxMasteryRank() {
        return MAX_MASTERY_RANK;
    }

    public int maxMasteryInvestedCopies() {
        return investedCopiesForRank(MAX_MASTERY_RANK);
    }

    public int getMasteryRank(SpellDeckProfile profile, SpellCardDefinition card) {
        if (profile == null || card == null) {
            return 0;
        }
        return getMasteryRank(profile.getInvestedCopies(card.cardId()));
    }

    public int getMasteryRank(UUID playerId, String spellId) {
        if (dataStore == null || playerId == null || spellId == null) {
            return 0;
        }
        SpellCardDefinition card = getDefinitionBySpellId(spellId);
        if (card == null) {
            return 0;
        }
        return getMasteryRank(dataStore.getProfile(playerId), card);
    }

    public double getMasteryManaCooldownMultiplier(UUID playerId, String spellId) {
        int rank = getMasteryRank(playerId, spellId);
        double reduction = Math.min(0.25, rank * MASTERY_MANA_COOLDOWN_REDUCTION_PER_RANK);
        return Math.max(0.0, 1.0 - reduction);
    }

    public int getMasteryProgress(SpellDeckProfile profile, SpellCardDefinition card) {
        if (profile == null || card == null) {
            return 0;
        }
        int invested = Math.min(profile.getInvestedCopies(card.cardId()), maxMasteryInvestedCopies());
        int rank = getMasteryRank(invested);
        if (rank >= MAX_MASTERY_RANK) {
            return getMasteryRequiredForNextRank(rank);
        }
        return invested - investedCopiesForRank(rank);
    }

    public int getMasteryRequiredForNextRank(int rank) {
        if (rank >= MAX_MASTERY_RANK) {
            return 0;
        }
        return rank + 1;
    }

    public int maxedDuplicateGemValue(SpellDeckRarity rarity) {
        return MAXED_DUPLICATE_GEMS.getOrDefault(rarity == null ? SpellDeckRarity.COMMON : rarity, 2);
    }

    private int getMasteryRank(int investedCopies) {
        int rank = 0;
        int safeInvested = Math.max(0, investedCopies);
        while (rank < MAX_MASTERY_RANK && safeInvested >= investedCopiesForRank(rank + 1)) {
            rank++;
        }
        return rank;
    }

    private int investedCopiesForRank(int rank) {
        int safeRank = Math.max(0, Math.min(MAX_MASTERY_RANK, rank));
        return safeRank * (safeRank + 1) / 2;
    }

    private void addGems(Player player, int gems) {
        if (player == null || gems <= 0 || plugin == null || plugin.getGemsManager() == null) {
            return;
        }
        plugin.getGemsManager().addUnits(player, gems);
    }

    private void autoEquipFirstCopy(Player player, SpellDeckProfile profile, SpellCardDefinition card) {
        if (player == null || profile == null || card == null) {
            return;
        }
        if (getEquippedSlotForFamily(profile, card.familyId()) != null) {
            return;
        }
        SpellInputType preferred = firstAvailableSpellSlot(profile, card.defaultInputType());
        if (preferred != null) {
            profile.equip(preferred, card.cardId());
        }
    }

    private SpellInputType firstAvailableSpellSlot(SpellDeckProfile profile, SpellInputType preferred) {
        List<SpellInputType> slots = List.of(SpellInputType.SPELL_1, SpellInputType.SPELL_2, SpellInputType.SPELL_3, SpellInputType.SPELL_4);
        if (preferred != null && slots.contains(preferred) && profile.getEquippedCardId(preferred) == null) {
            return preferred;
        }
        for (SpellInputType slot : slots) {
            if (profile.getEquippedCardId(slot) == null) {
                return slot;
            }
        }
        return null;
    }

    private SpellCardDefinition rollCard(Random random,
                                         Map<SpellDeckRarity, List<SpellCardDefinition>> pools,
                                         Map<SpellDeckRarity, Double> weights,
                                         boolean pityGuaranteed) {
        SpellDeckRarity rarity = null;
        if (pityGuaranteed) {
            rarity = pickWeightedRarityAtOrAbove(random, pools, SpellDeckRarity.LEGENDARY);
        }
        if (rarity == null) {
            rarity = RandomUtil.pickWeighted(random, weights);
        }
        List<SpellCardDefinition> options = pools.get(rarity);
        if (options == null || options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }

    private SpellDeckRarity pickWeightedRarityAtOrAbove(Random random,
                                                        Map<SpellDeckRarity, List<SpellCardDefinition>> pools,
                                                        SpellDeckRarity minimum) {
        Map<SpellDeckRarity, Double> eligible = new EnumMap<>(SpellDeckRarity.class);
        for (SpellDeckRarity rarity : GACHA_RARITIES) {
            if (rarity.ordinal() < minimum.ordinal()) {
                continue;
            }
            List<SpellCardDefinition> options = pools.get(rarity);
            if (options == null || options.isEmpty()) {
                continue;
            }
            eligible.put(rarity, GACHA_WEIGHTS.getOrDefault(rarity, 1.0));
        }
        return RandomUtil.pickWeighted(random, eligible);
    }

    private Map<SpellDeckRarity, List<SpellCardDefinition>> buildRarityPools() {
        Map<SpellDeckRarity, List<SpellCardDefinition>> pools = new EnumMap<>(SpellDeckRarity.class);
        for (SpellCardDefinition definition : definitions.values()) {
            pools.computeIfAbsent(definition.rarity(), ignored -> new ArrayList<>()).add(definition);
        }
        return pools;
    }

    private Map<SpellDeckRarity, Double> buildRarityWeights(Map<SpellDeckRarity, List<SpellCardDefinition>> pools) {
        Map<SpellDeckRarity, Double> weights = new EnumMap<>(SpellDeckRarity.class);
        for (Map.Entry<SpellDeckRarity, List<SpellCardDefinition>> entry : pools.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            weights.put(entry.getKey(), GACHA_WEIGHTS.getOrDefault(entry.getKey(), 1.0));
        }
        return weights;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record SpellPullEntry(SpellCardDefinition card, boolean duplicateInvested, int gemsSalvaged) {
        public SpellPullEntry(SpellCardDefinition card) {
            this(card, false, 0);
        }
    }
    public record SpellPullResult(List<SpellPullEntry> pulls,
                                  Map<SpellCardDefinition, Integer> summary,
                                  int duplicateInvestments,
                                  int salvagedGems) {}
    public record InvestAllResult(int cardsTouched, int copiesInvested, int gemsSalvaged) {}
}

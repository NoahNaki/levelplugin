package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.PullLevelProgression;
import me.nakilex.levelplugin.utils.RandomUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final Map<SpellDeckRarity, Integer> MASTERY_VALUE_BY_RARITY = Map.of(
            SpellDeckRarity.COMMON, 1,
            SpellDeckRarity.UNCOMMON, 2,
            SpellDeckRarity.RARE, 3,
            SpellDeckRarity.EPIC, 5,
            SpellDeckRarity.LEGENDARY, 8,
            SpellDeckRarity.MYTHIC, 12
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
        registerClassSpellCards();
    }

    private void registerClassSpellCards() {
        registerCard("meteor", "meteor", "meteor", "Meteor",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Calls a falling meteor onto your target area.",
                List.of("Damage: 18", "Impact Radius: 3.8 blocks", "Burn: 6s"), List.of());
        registerCard("blackhole", "blackhole", "blackhole", "Blackhole",
                SpellDeckRarity.COMMON, SpellCardCategory.UTILITY, SpellInputType.SPELL_1,
                "Creates a gravity well that pulls enemies inward.",
                List.of("Radius: 4.2 blocks", "Pull Strength: 1.7", "Duration: 60 ticks"), List.of());
        registerCard("arcane_mend", "arcane_mend", "mage_heal", "Arcane Mend",
                SpellDeckRarity.COMMON, SpellCardCategory.SUPPORT, SpellInputType.SPELL_4,
                "Restores health with a quick arcane pulse.",
                List.of("Healing: 8", "Bonus Health: 8"), List.of());

        registerCard("seeker_barrage", "seeker_barrage", "archer_homing_barrage", "Seeker Barrage",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Fires a volley of arrows that seek enemies.",
                List.of("Arrows: 9", "Delay: 2 ticks", "Search Radius: 4.8 blocks"), List.of());
        registerCard("arrow_rain", "arrow_rain", "archer_arrow_rain", "Arrow Rain",
                SpellDeckRarity.MYTHIC, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Rains arrows over a large target area.",
                List.of("Arrows: 8", "Waves: 11", "Radius: 8.2 blocks"), List.of());
        registerCard("windguard", "windguard", "archer_windguard", "Windguard",
                SpellDeckRarity.RARE, SpellCardCategory.DEFENSIVE, SpellInputType.SPELL_4,
                "Summons wind to guard you from incoming damage.",
                List.of("Shield: 100", "Charges: 1", "Duration: 30s"), List.of());

        registerCard("shadow_flurry", "shadow_flurry", "rogue_sky_ripper", "Shadow Flurry",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Strikes enemies with rapid shadow slashes.",
                List.of("Strikes: 4", "Radius: 6 blocks", "Damage: 7.4"), List.of());
        registerCard("nightfall_lunge", "nightfall_lunge", "rogue_phantom_cross", "Nightfall Lunge",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Lunges through enemies with phantom cuts.",
                List.of("Slashes: 4", "Range: 6 blocks", "Damage: 11.8"), List.of());
        registerCard("smoke_bomb", "smoke_bomb", "rogue_veil_counter", "Smoke Bomb",
                SpellDeckRarity.COMMON, SpellCardCategory.DEFENSIVE, SpellInputType.SPELL_4,
                "Drops smoke that protects you and weakens enemies.",
                List.of("Duration: 16s", "Radius: 2 blocks", "Shield: 100"), List.of());

        registerCard("earthquake", "earthquake", "warrior_earthquake", "Earthquake",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Shatters the ground around your target area.",
                List.of("Radius: 3.8 blocks", "Damage: 6.2"), List.of());
        registerCard("rupture_cyclone", "rupture_cyclone", "warrior_rupture_cyclone", "Rupture Cyclone",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Spins through enemies and applies rupturing pressure.",
                List.of("Hits: 9", "Radius: 3.8 blocks", "Damage Multiplier: 1x"), List.of());
        registerCard("aegis_bastion", "aegis_bastion", "warrior_guarded_resolve", "Aegis Bastion",
                SpellDeckRarity.RARE, SpellCardCategory.DEFENSIVE, SpellInputType.SPELL_4,
                "Raises a bastion that absorbs incoming damage.",
                List.of("Shield: 130", "Duration: 5s", "Guard Radius: 34 blocks"), List.of());
        registerCard("cyclone_brand", "cyclone_brand", "warrior_execution_arc", "Cyclone Brand",
                SpellDeckRarity.LEGENDARY, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Brands enemies with a sweeping execution arc.",
                List.of("Damage: 120", "Radius: 6.4 blocks"), List.of());
    }

    private void registerCard(String cardId,
                              String familyId,
                              String spellId,
                              String displayName,
                              SpellDeckRarity rarity,
                              SpellCardCategory category,
                              SpellInputType defaultInputType,
                              String description,
                              List<String> effectLines,
                              List<String> tradeoffLines) {
        List<String> lore = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            lore.add(description);
        }
        if (effectLines != null) {
            lore.addAll(effectLines);
        }
        register(new SpellCardDefinition(cardId, familyId, spellId, displayName, rarity, category,
                defaultInputType, null, lore, tradeoffLines == null ? List.of() : tradeoffLines));
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
        return SpellRegistry.getInstance().getSpell(getEffectiveSpellId(player.getUniqueId(), card));
    }

    public String getEffectiveSpellId(UUID playerId, SpellCardDefinition card) {
        if (card == null) {
            return null;
        }
        int masteryRank = getMasteryRank(getProfile(playerId), card);
        return me.nakilex.levelplugin.spells.progression.SpellProgressionManager.getInstance()
                .getSpellIdAtLevel(card.spellId(), masteryRank);
    }

    public String getEffectiveSpellId(UUID playerId, String spellId) {
        SpellCardDefinition card = getDefinitionBySpellId(spellId);
        return card == null ? spellId : getEffectiveSpellId(playerId, card);
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
            return SpellPullResult.empty();
        }
        SpellDeckProfile profile = dataStore.getProfile(player.getUniqueId());
        Map<SpellDeckRarity, List<SpellCardDefinition>> pools = buildRarityPools();
        List<SpellPullEntry> entries = new ArrayList<>(amount);
        Map<SpellCardDefinition, Integer> unlocked = new LinkedHashMap<>();
        Map<SpellCardDefinition, Integer> masteryGained = new LinkedHashMap<>();
        Map<SpellCardDefinition, Integer> autoDiscarded = new LinkedHashMap<>();
        int salvagedGems = 0;
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            boolean pityGuaranteed = profile.pityPullsSinceLegendary() >= (PITY_THRESHOLD - 1);
            Map<SpellDeckRarity, Double> weights = buildRarityWeights(pools, getBannerLevel(player.getUniqueId()));
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
                unlocked.merge(card, 1, Integer::sum);
                entries.add(new SpellPullEntry(card, SpellPullOutcome.UNLOCKED, 0, 0));
            } else if (invested < maxMasteryInvestedCopies()) {
                int masteryValue = Math.min(masteryValue(card.rarity()), maxMasteryInvestedCopies() - invested);
                profile.addInvestedCopies(card.cardId(), masteryValue);
                masteryGained.merge(card, masteryValue, Integer::sum);
                entries.add(new SpellPullEntry(card, SpellPullOutcome.MASTERY_GAINED, masteryValue, 0));
            } else {
                int gems = maxedDuplicateGemValue(card.rarity());
                addGems(player, gems);
                salvagedGems += gems;
                autoDiscarded.merge(card, 1, Integer::sum);
                entries.add(new SpellPullEntry(card, SpellPullOutcome.AUTO_DISCARDED, 0, gems));
            }
            profile.addBannerPulls(1);
        }
        dataStore.saveProfile(player.getUniqueId());
        return new SpellPullResult(entries, unlocked, masteryGained, autoDiscarded, salvagedGems);
    }

    public List<SpellDeckRarity> getGachaRarities() {
        return GACHA_RARITIES;
    }

    public Map<SpellDeckRarity, Double> getGachaRates() {
        return Collections.unmodifiableMap(GACHA_WEIGHTS);
    }

    public Map<SpellDeckRarity, Double> getGachaRates(UUID playerId) {
        return Collections.unmodifiableMap(PullLevelProgression.ratesForLevel(
                GACHA_RARITIES, GACHA_WEIGHTS, getBannerLevel(playerId)));
    }

    public int getBannerLevel(UUID playerId) {
        return PullLevelProgression.levelForPulls(getBannerPulls(playerId));
    }

    public int getBannerLevelProgress(UUID playerId) {
        return PullLevelProgression.progressIntoLevel(getBannerPulls(playerId));
    }

    public int getBannerLevelRequirement(UUID playerId) {
        return PullLevelProgression.requiredForNextLevel(getBannerLevel(playerId));
    }

    public int getMaxBannerLevel() {
        return PullLevelProgression.MAX_LEVEL;
    }

    private int getBannerPulls(UUID playerId) {
        if (dataStore == null || playerId == null) {
            return 0;
        }
        return dataStore.getProfile(playerId).bannerPulls();
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

    public int masteryValue(SpellDeckRarity rarity) {
        return MASTERY_VALUE_BY_RARITY.getOrDefault(rarity == null ? SpellDeckRarity.COMMON : rarity, 1);
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
        if (eligible.isEmpty()) {
            return null;
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

    private Map<SpellDeckRarity, Double> buildRarityWeights(Map<SpellDeckRarity, List<SpellCardDefinition>> pools, int bannerLevel) {
        Map<SpellDeckRarity, Double> levelRates = PullLevelProgression.ratesForLevel(GACHA_RARITIES, GACHA_WEIGHTS, bannerLevel);
        Map<SpellDeckRarity, Double> weights = new EnumMap<>(SpellDeckRarity.class);
        for (Map.Entry<SpellDeckRarity, List<SpellCardDefinition>> entry : pools.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            weights.put(entry.getKey(), levelRates.getOrDefault(entry.getKey(), 0.0));
        }
        return weights;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public enum SpellPullOutcome {
        UNLOCKED,
        MASTERY_GAINED,
        AUTO_DISCARDED
    }

    public record SpellPullEntry(SpellCardDefinition card,
                                 SpellPullOutcome outcome,
                                 int masteryGained,
                                 int gemsSalvaged) {
    }

    public record SpellPullResult(List<SpellPullEntry> pulls,
                                  Map<SpellCardDefinition, Integer> unlocked,
                                  Map<SpellCardDefinition, Integer> masteryGained,
                                  Map<SpellCardDefinition, Integer> autoDiscarded,
                                  int salvagedGems) {
        public SpellPullResult {
            pulls = pulls == null ? List.of() : List.copyOf(pulls);
            unlocked = unlocked == null ? Map.of() : Map.copyOf(unlocked);
            masteryGained = masteryGained == null ? Map.of() : Map.copyOf(masteryGained);
            autoDiscarded = autoDiscarded == null ? Map.of() : Map.copyOf(autoDiscarded);
            salvagedGems = Math.max(0, salvagedGems);
        }

        public static SpellPullResult empty() {
            return new SpellPullResult(List.of(), Map.of(), Map.of(), Map.of(), 0);
        }

        public boolean isEmpty() {
            return unlocked.isEmpty() && masteryGained.isEmpty() && autoDiscarded.isEmpty();
        }

        public int totalInvested() {
            return masteryGained.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    public record InvestAllResult(int cardsTouched, int copiesInvested, int gemsSalvaged) {}
}

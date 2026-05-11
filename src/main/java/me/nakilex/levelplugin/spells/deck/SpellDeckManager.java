package me.nakilex.levelplugin.spells.deck;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellProgression;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.PullLevelProgression;
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
    private static final Map<SpellDeckRarity, Integer> PULL_MASTERY_VALUES = Map.of(
            SpellDeckRarity.COMMON, 1,
            SpellDeckRarity.UNCOMMON, 1,
            SpellDeckRarity.RARE, 2,
            SpellDeckRarity.EPIC, 3,
            SpellDeckRarity.LEGENDARY, 5,
            SpellDeckRarity.MYTHIC, 8
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
        register(new SpellCardDefinition("fireball", "fireball", "deck_fireball_common", "Fireball",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1, null,
                List.of("Shoots a fireball forward.", "Duplicate pulls raise mastery and unlock Enhanced, Infernal, Cataclysm, Dragonfire, and Worldfire tiers.", "Upgrades reuse one card asset and spell family."),
                List.of()));
        registerClassSpellCards();
    }

    private void registerClassSpellCards() {
        registerCard("meteor", "meteor", "meteor", "Meteor",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Calls a falling meteor onto your target area.",
                List.of("Duplicate pulls raise mastery and unlock Emberfall/Cataclysm tiers.", "Upgrades reuse the same card icon and spell family."), List.of());
        registerCard("blackhole", "blackhole", "blackhole", "Blackhole",
                SpellDeckRarity.RARE, SpellCardCategory.UTILITY, SpellInputType.SPELL_1,
                "Creates a gravity well that pulls enemies inward.",
                List.of("Duplicate pulls raise mastery and unlock Gravity Well/Singularity tiers."), List.of());
        registerCard("arcane_mend", "arcane_mend", "mage_heal", "Arcane Mend",
                SpellDeckRarity.RARE, SpellCardCategory.SUPPORT, SpellInputType.SPELL_4,
                "Restores health with a quick arcane pulse.",
                List.of("Duplicate pulls raise mastery and unlock Rejuvenating/Party Mend tiers."), List.of());

        registerCard("seeker_barrage", "seeker_barrage", "archer_homing_barrage", "Seeker Barrage",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Fires homing arrows at nearby enemies.",
                List.of("Mastery improves mana and cooldown efficiency without new card assets."), List.of());
        registerCard("arrow_rain", "arrow_rain", "archer_arrow_rain", "Arrow Rain",
                SpellDeckRarity.EPIC, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Rains arrows over a targeted area.",
                List.of("Mastery improves mana and cooldown efficiency without new card assets."), List.of());
        registerCard("windguard", "windguard", "archer_windguard", "Windguard",
                SpellDeckRarity.RARE, SpellCardCategory.SUPPORT, SpellInputType.SPELL_4,
                "Creates a defensive wind barrier.",
                List.of("Mastery improves mana and cooldown efficiency without new card assets."), List.of());

        registerCard("shadow_flurry", "shadow_flurry", "rogue_sky_ripper", "Shadow Flurry",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Dashes through enemies with a rapid slash.",
                List.of("Duplicate pulls raise mastery and unlock Tempest/Execution tiers."), List.of());
        registerCard("nightfall_lunge", "nightfall_lunge", "rogue_phantom_cross", "Nightfall Lunge",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Lunges forward and cuts enemies in your path.",
                List.of("Duplicate pulls raise mastery and unlock Cyclone/Judgement tiers."), List.of());
        registerCard("smoke_bomb", "smoke_bomb", "rogue_veil_counter", "Smoke Bomb",
                SpellDeckRarity.COMMON, SpellCardCategory.UTILITY, SpellInputType.SPELL_4,
                "Drops a smoke veil that protects your escape.",
                List.of("Duplicate pulls raise mastery and unlock Obscure/Dread tiers."), List.of());

        registerCard("earthquake", "earthquake", "warrior_earthquake", "Earthquake",
                SpellDeckRarity.COMMON, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Slams the ground and damages nearby enemies.",
                List.of("Duplicate pulls raise mastery and unlock Tremor/Cataclysm tiers."), List.of());
        registerCard("rupture_cyclone", "rupture_cyclone", "warrior_rupture_cyclone", "Rupture Cyclone",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Spins forward and repeatedly damages enemies.",
                List.of("Mastery improves mana and cooldown efficiency without new card assets."), List.of());
        registerCard("aegis_bastion", "aegis_bastion", "warrior_guarded_resolve", "Aegis Bastion",
                SpellDeckRarity.RARE, SpellCardCategory.SUPPORT, SpellInputType.SPELL_4,
                "Grants a short defensive bulwark.",
                List.of("Mastery improves mana and cooldown efficiency without new card assets."), List.of());
        registerCard("cyclone_brand", "cyclone_brand", "warrior_execution_arc", "Cyclone Brand",
                SpellDeckRarity.RARE, SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Brands enemies with a sweeping execution arc.",
                List.of("Mastery improves mana and cooldown efficiency without new card assets."), List.of());
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
        SpellProgression progression = SpellRegistry.getInstance().getProgression(definition.spellId());
        if (progression != null) {
            for (String upgradeSpellId : progression.upgradeSpellIds()) {
                definitionsBySpellId.putIfAbsent(normalize(upgradeSpellId), definition);
            }
        }
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
            if (getCopies(profile, definition) > 0) {
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
        return definitions.get(canonicalCardId(cardId));
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
        String effectiveSpellId = getEffectiveCardSpellId(player.getUniqueId(), card);
        return SpellRegistry.getInstance().getSpell(effectiveSpellId);
    }

    public String getEffectiveCardSpellId(UUID playerId, SpellCardDefinition card) {
        if (playerId == null || card == null) {
            return null;
        }
        SpellDeckProfile profile = getProfile(playerId);
        int masteryRank = getMasteryRank(profile, card);
        return SpellProgressionManager.getInstance().getSpellIdAtLevel(card.spellId(), masteryRank);
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
        SpellDeckProfile profile = dataStore.getProfile(playerId);
        SpellCardDefinition definition = getDefinition(cardId);
        return definition == null ? profile.getCopies(cardId) : getCopies(profile, definition);
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
        if (getCopies(profile, definition) <= 0) {
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

    private int getCopies(SpellDeckProfile profile, SpellCardDefinition card) {
        if (profile == null || card == null) {
            return 0;
        }
        return totalForCanonical(profile.ownedCopies(), card.cardId());
    }

    private int getInvestedCopies(SpellDeckProfile profile, SpellCardDefinition card) {
        if (profile == null || card == null) {
            return 0;
        }
        return totalForCanonical(profile.investedCopies(), card.cardId());
    }

    private void setCanonicalCopies(SpellDeckProfile profile, SpellCardDefinition card, int copies) {
        if (profile == null || card == null) {
            return;
        }
        String canonical = canonicalCardId(card.cardId());
        profile.ownedCopies().keySet().removeIf(cardId -> canonicalCardId(cardId).equals(canonical));
        profile.setCopies(canonical, copies);
    }

    private int totalForCanonical(Map<String, Integer> values, String cardId) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        String canonical = canonicalCardId(cardId);
        int total = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (canonicalCardId(entry.getKey()).equals(canonical)) {
                total += Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        return total;
    }

    private String canonicalCardId(String cardId) {
        String normalized = normalize(cardId);
        if (definitions.containsKey(normalized)) {
            return normalized;
        }
        for (SpellDeckRarity rarity : SpellDeckRarity.values()) {
            String suffix = "_" + rarity.name().toLowerCase(Locale.ROOT);
            if (normalized.endsWith(suffix)) {
                String base = normalized.substring(0, normalized.length() - suffix.length());
                if (definitions.containsKey(base)) {
                    return base;
                }
            }
        }
        return normalized;
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
        List<SpellPullEntry> entries = new ArrayList<>(amount);
        Map<SpellCardDefinition, Integer> summary = new HashMap<>();
        int duplicateInvestments = 0;
        int salvagedGems = 0;
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            boolean pityGuaranteed = profile.pityPullsSinceLegendary() >= (PITY_THRESHOLD - 1);
            Map<SpellDeckRarity, Double> weights = PullLevelProgression.ratesForLevel(
                    GACHA_RARITIES, GACHA_WEIGHTS, getBannerLevel(player.getUniqueId()));
            SpellDeckRarity pulledRarity = pityGuaranteed
                    ? pickWeightedRarityAtOrAbove(random, SpellDeckRarity.LEGENDARY)
                    : RandomUtil.pickWeighted(random, weights);
            if (pulledRarity == null) {
                pulledRarity = SpellDeckRarity.COMMON;
            }
            SpellCardDefinition card = rollCard(random, pools, pulledRarity);
            if (card == null) {
                continue;
            }
            if (pulledRarity.ordinal() >= SpellDeckRarity.LEGENDARY.ordinal()) {
                profile.setPityPullsSinceLegendary(0);
            } else {
                profile.setPityPullsSinceLegendary(profile.pityPullsSinceLegendary() + 1);
            }
            int remainingMasteryValue = pullMasteryValue(pulledRarity);
            int existingCopies = getCopies(profile, card);
            if (existingCopies <= 0) {
                profile.addCopies(card.cardId(), 1);
                remainingMasteryValue--;
                autoEquipFirstCopy(player, profile, card);
                entries.add(new SpellPullEntry(card));
            }
            if (remainingMasteryValue > 0) {
                int availableMastery = Math.max(0, maxMasteryInvestedCopies() - getInvestedCopies(profile, card));
                int investedNow = Math.min(remainingMasteryValue, availableMastery);
                int salvageValue = remainingMasteryValue - investedNow;
                if (investedNow > 0) {
                    profile.addInvestedCopies(card.cardId(), investedNow);
                    duplicateInvestments += investedNow;
                }
                if (salvageValue > 0) {
                    int gems = salvageValue * maxedDuplicateGemValue(pulledRarity);
                    addGems(player, gems);
                    salvagedGems += gems;
                    entries.add(new SpellPullEntry(card, investedNow > 0, gems));
                } else if (existingCopies > 0) {
                    entries.add(new SpellPullEntry(card, investedNow > 0, 0));
                }
            }
            profile.addBannerPulls(1);
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
            int copies = getCopies(profile, definition);
            if (copies <= 1) {
                continue;
            }
            int duplicateCopies = copies - 1;
            int availableMastery = Math.max(0, maxMasteryInvestedCopies() - getInvestedCopies(profile, definition));
            int investableCopies = Math.min(duplicateCopies, availableMastery);
            int salvageCopies = duplicateCopies - investableCopies;
            setCanonicalCopies(profile, definition, 1);
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
        return getMasteryRank(getInvestedCopies(profile, card));
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
        int invested = Math.min(getInvestedCopies(profile, card), maxMasteryInvestedCopies());
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
                                         SpellDeckRarity pulledRarity) {
        List<SpellCardDefinition> options = new ArrayList<>();
        SpellDeckRarity safeRarity = pulledRarity == null ? SpellDeckRarity.COMMON : pulledRarity;
        for (SpellDeckRarity rarity : GACHA_RARITIES) {
            if (rarity.ordinal() > safeRarity.ordinal()) {
                continue;
            }
            options.addAll(pools.getOrDefault(rarity, List.of()));
        }
        if (options.isEmpty()) {
            for (List<SpellCardDefinition> fallback : pools.values()) {
                options.addAll(fallback);
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }

    private SpellDeckRarity pickWeightedRarityAtOrAbove(Random random, SpellDeckRarity minimum) {
        Map<SpellDeckRarity, Double> eligible = new EnumMap<>(SpellDeckRarity.class);
        SpellDeckRarity safeMinimum = minimum == null ? SpellDeckRarity.COMMON : minimum;
        for (SpellDeckRarity rarity : GACHA_RARITIES) {
            if (rarity.ordinal() < safeMinimum.ordinal()) {
                continue;
            }
            eligible.put(rarity, GACHA_WEIGHTS.getOrDefault(rarity, 1.0));
        }
        return RandomUtil.pickWeighted(random, eligible);
    }

    private int pullMasteryValue(SpellDeckRarity rarity) {
        return PULL_MASTERY_VALUES.getOrDefault(rarity == null ? SpellDeckRarity.COMMON : rarity, 1);
    }

    private Map<SpellDeckRarity, List<SpellCardDefinition>> buildRarityPools() {
        Map<SpellDeckRarity, List<SpellCardDefinition>> pools = new EnumMap<>(SpellDeckRarity.class);
        for (SpellCardDefinition definition : definitions.values()) {
            pools.computeIfAbsent(definition.rarity(), ignored -> new ArrayList<>()).add(definition);
        }
        return pools;
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

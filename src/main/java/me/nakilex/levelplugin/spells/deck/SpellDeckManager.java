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
    private static final int MAX_MASTERY_LEVEL = 5;
    private static final double MASTERY_MANA_COOLDOWN_REDUCTION_PER_LEVEL = 0.02;
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
        registerRarityTrack("meteor", "meteor", "Meteor", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Calls a falling meteor onto your target area.",
                List.of("Damage: 18", "Impact Radius: 3.8 blocks", "Burn: 6s"),
                List.of("Damage: 20", "Impact Radius: 4.4 blocks", "Cooldown: -0.2s"),
                List.of("Damage: 22", "Impact Radius: 5.8 blocks", "Secondary Meteors: 3"),
                List.of("Damage: 27", "Impact Radius: 7.4 blocks", "Secondary Meteors: 5", "Added effect: scorched impact zone"),
                List.of("Damage: 31", "Impact Radius: 8.4 blocks", "Secondary Meteors: 7", "Added effect: enemies are briefly slowed"),
                List.of("Damage: 36", "Impact Radius: 9.6 blocks", "Secondary Meteors: 9", "Added effect: final meteor detonates twice"));
        registerRarityTrack("blackhole", "blackhole", "Blackhole", SpellCardCategory.UTILITY, SpellInputType.SPELL_1,
                "Creates a gravity well that pulls enemies inward.",
                List.of("Radius: 4.2 blocks", "Pull Strength: 1.7", "Duration: 60 ticks"),
                List.of("Radius: 5.0 blocks", "Pull Strength: 2.1", "Mana Cost: -2"),
                List.of("Radius: 6.8 blocks", "Pull Strength: 2.9", "Pulses: 2"),
                List.of("Radius: 9 blocks", "Pull Strength: 4", "Pulses: 4", "Added effect: compression damage"),
                List.of("Radius: 10.5 blocks", "Pull Strength: 4.6", "Pulses: 5", "Added effect: applies vulnerability"),
                List.of("Radius: 12 blocks", "Pull Strength: 5.2", "Pulses: 6", "Added effect: final singularity burst"));
        registerRarityTrack("arcane_mend", "mage_heal", "Arcane Mend", SpellCardCategory.SUPPORT, SpellInputType.SPELL_4,
                "Restores health with a quick arcane pulse.",
                List.of("Healing: 8", "Bonus Health: 8"),
                List.of("Healing: 10", "Bonus Health: 14", "Mana Cost: -1"),
                List.of("Healing: 14", "Regen Duration: 2s", "Bonus Health: 22"),
                List.of("Healing: 12", "Party Heal: enabled", "Regen Duration: 2s", "Added effect: nearby ally pulse"),
                List.of("Healing: 16", "Party Heal: stronger", "Regen Duration: 3s", "Added effect: mana restore pulse"),
                List.of("Healing: 20", "Party Heal: strongest", "Regen Duration: 4s", "Added effect: emergency shield"));

        registerRarityTrack("seeker_barrage", "archer_homing_barrage", "Seeker Barrage", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Fires a volley of arrows that seek enemies.",
                List.of("Arrows: 9", "Delay: 2 ticks", "Search Radius: 4.8 blocks"),
                List.of("Arrows: 10", "Search Radius: 5.4 blocks", "Cooldown: -0.2s"),
                List.of("Arrows: 12", "Search Radius: 6.2 blocks", "Damage: +12%"),
                List.of("Arrows: 14", "Search Radius: 7 blocks", "Added effect: arrows pierce once"),
                List.of("Arrows: 16", "Search Radius: 7.8 blocks", "Added effect: marked target takes bonus hits"),
                List.of("Arrows: 18", "Search Radius: 8.6 blocks", "Added effect: final arrow explodes"));
        registerRarityTrack("arrow_rain", "archer_arrow_rain", "Arrow Rain", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Rains arrows over a target area.",
                List.of("Arrows: 8", "Waves: 11", "Radius: 8.2 blocks"),
                List.of("Arrows: 9", "Waves: 11", "Cooldown: -0.3s"),
                List.of("Arrows: 10", "Waves: 12", "Radius: 9 blocks"),
                List.of("Arrows: 11", "Waves: 13", "Added effect: volley pins enemies"),
                List.of("Arrows: 12", "Waves: 14", "Added effect: focused center strike"),
                List.of("Arrows: 14", "Waves: 15", "Added effect: storm finisher"));
        registerRarityTrack("windguard", "archer_windguard", "Windguard", SpellCardCategory.DEFENSIVE, SpellInputType.SPELL_4,
                "Summons wind to guard you from incoming damage.",
                List.of("Shield: 100", "Charges: 1", "Duration: 30s"),
                List.of("Shield: 110", "Charges: 1", "Cooldown: -0.4s"),
                List.of("Shield: 125", "Charges: 2", "Duration: 34s"),
                List.of("Shield: 145", "Charges: 2", "Added effect: speed burst on block"),
                List.of("Shield: 165", "Charges: 3", "Added effect: nearby allies gain windguard"),
                List.of("Shield: 190", "Charges: 3", "Added effect: reflects a projectile"));

        registerRarityTrack("shadow_flurry", "rogue_sky_ripper", "Shadow Flurry", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Strikes enemies with rapid shadow slashes.",
                List.of("Strikes: 4", "Radius: 6 blocks", "Damage: 7.4"),
                List.of("Strikes: 5", "Radius: 6.6 blocks", "Cooldown: -0.2s"),
                List.of("Strikes: 6", "Radius: 7.6 blocks", "Damage: 13"),
                List.of("Strikes: 8", "Radius: 8.9 blocks", "Damage: 17.6", "Added effect: execution drop"),
                List.of("Strikes: 9", "Radius: 9.6 blocks", "Added effect: applies shadow mark"),
                List.of("Strikes: 10", "Radius: 10.4 blocks", "Added effect: marked enemies detonate"));
        registerRarityTrack("nightfall_lunge", "rogue_phantom_cross", "Nightfall Lunge", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Lunges through enemies with phantom cuts.",
                List.of("Slashes: 4", "Range: 6 blocks", "Damage: 11.8"),
                List.of("Slashes: 5", "Range: 6.8 blocks", "Mana Cost: -1"),
                List.of("Slashes: 6", "Range: 7.5 blocks", "Damage: 17"),
                List.of("Slashes: 8", "Range: 8.6 blocks", "Damage: 23", "Added effect: judgement strike"),
                List.of("Slashes: 9", "Range: 9.4 blocks", "Added effect: refreshes on takedown"),
                List.of("Slashes: 10", "Range: 10.2 blocks", "Added effect: phantom afterimage repeats"));
        registerRarityTrack("smoke_bomb", "rogue_veil_counter", "Smoke Bomb", SpellCardCategory.DEFENSIVE, SpellInputType.SPELL_4,
                "Drops smoke that protects you and weakens enemies.",
                List.of("Duration: 16s", "Radius: 2 blocks", "Shield: 100"),
                List.of("Duration: 18s", "Radius: 2.1 blocks", "Cooldown: -0.4s"),
                List.of("Duration: 22s", "Radius: 2.3 blocks", "Shield: 120"),
                List.of("Duration: 30s", "Radius: 2.8 blocks", "Shield: 140", "Added effect: dread cloud"),
                List.of("Duration: 34s", "Radius: 3.1 blocks", "Added effect: enemies are silenced briefly"),
                List.of("Duration: 38s", "Radius: 3.4 blocks", "Added effect: vanish burst on cast"));

        registerRarityTrack("earthquake", "warrior_earthquake", "Earthquake", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Shatters the ground around your target area.",
                List.of("Radius: 3.8 blocks", "Damage: 6.2"),
                List.of("Radius: 4.8 blocks", "Damage: 8.1", "Cooldown: -0.2s"),
                List.of("Radius: 6.3 blocks", "Damage: 10.8"),
                List.of("Radius: 8.8 blocks", "Damage: 14.2", "Added effect: cataclysm shockwave"),
                List.of("Radius: 9.8 blocks", "Damage: 16.4", "Added effect: armor fracture"),
                List.of("Radius: 11 blocks", "Damage: 19", "Added effect: second quake aftershock"));
        registerRarityTrack("rupture_cyclone", "warrior_rupture_cyclone", "Rupture Cyclone", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_2,
                "Spins through enemies and applies rupturing pressure.",
                List.of("Hits: 9", "Radius: 3.8 blocks", "Damage Multiplier: 1x"),
                List.of("Hits: 10", "Radius: 4.2 blocks", "Mana Cost: -1"),
                List.of("Hits: 11", "Radius: 4.8 blocks", "Damage Multiplier: 1.15x"),
                List.of("Hits: 12", "Radius: 5.4 blocks", "Added effect: bleed stacks"),
                List.of("Hits: 13", "Radius: 6.0 blocks", "Added effect: pulls enemies inward"),
                List.of("Hits: 15", "Radius: 6.8 blocks", "Added effect: rupture detonation"));
        registerRarityTrack("aegis_bastion", "warrior_guarded_resolve", "Aegis Bastion", SpellCardCategory.DEFENSIVE, SpellInputType.SPELL_4,
                "Raises a bastion that absorbs incoming damage.",
                List.of("Shield: 130", "Duration: 5s", "Guard Radius: 34 blocks"),
                List.of("Shield: 145", "Duration: 5.5s", "Cooldown: -0.4s"),
                List.of("Shield: 165", "Duration: 6s", "Blocked Hits: +1"),
                List.of("Shield: 190", "Duration: 6.5s", "Added effect: party guard pulse"),
                List.of("Shield: 220", "Duration: 7s", "Added effect: fortifies nearby allies"),
                List.of("Shield: 260", "Duration: 8s", "Added effect: retaliatory shockwave"));
        registerRarityTrack("cyclone_brand", "warrior_execution_arc", "Cyclone Brand", SpellCardCategory.OFFENSIVE, SpellInputType.SPELL_1,
                "Brands enemies with a sweeping execution arc.",
                List.of("Damage: 120", "Radius: 6.4 blocks"),
                List.of("Damage: 135", "Radius: 6.9 blocks", "Cooldown: -0.2s"),
                List.of("Damage: 155", "Radius: 7.5 blocks", "Duration: +10 ticks"),
                List.of("Damage: 180", "Radius: 8.2 blocks", "Added effect: execute low health enemies"),
                List.of("Damage: 215", "Radius: 9 blocks", "Added effect: brands spread once"),
                List.of("Damage: 260", "Radius: 10 blocks", "Added effect: cyclone repeats"));
    }

    private void registerRarityTrack(String familyId,
                                     String baseSpellId,
                                     String baseName,
                                     SpellCardCategory category,
                                     SpellInputType defaultInputType,
                                     String description,
                                     List<String> commonEffects,
                                     List<String> uncommonEffects,
                                     List<String> rareEffects,
                                     List<String> epicEffects,
                                     List<String> legendaryEffects,
                                     List<String> mythicEffects) {
        registerRarityCard(familyId, baseSpellId, baseName, SpellDeckRarity.COMMON, category, defaultInputType, description, commonEffects);
        registerRarityCard(familyId, baseSpellId, baseName, SpellDeckRarity.UNCOMMON, category, defaultInputType, description, uncommonEffects);
        registerRarityCard(familyId, baseSpellId, baseName, SpellDeckRarity.RARE, category, defaultInputType, description, rareEffects);
        registerRarityCard(familyId, baseSpellId, baseName, SpellDeckRarity.EPIC, category, defaultInputType, description, epicEffects);
        registerRarityCard(familyId, baseSpellId, baseName, SpellDeckRarity.LEGENDARY, category, defaultInputType, description, legendaryEffects);
        registerRarityCard(familyId, baseSpellId, baseName, SpellDeckRarity.MYTHIC, category, defaultInputType, description, mythicEffects);
    }

    private void registerRarityCard(String familyId,
                                    String baseSpellId,
                                    String baseName,
                                    SpellDeckRarity rarity,
                                    SpellCardCategory category,
                                    SpellInputType defaultInputType,
                                    String description,
                                    List<String> effectLines) {
        registerCard(cardIdForRarity(familyId, rarity), familyId, baseSpellId, displayNameForRarity(baseName, rarity),
                rarity, category, defaultInputType, description, effectLines, List.of());
    }

    private String cardIdForRarity(String familyId, SpellDeckRarity rarity) {
        String normalizedFamily = normalize(familyId);
        if (rarity == null || rarity == SpellDeckRarity.COMMON) {
            return normalizedFamily;
        }
        return normalizedFamily + "_" + rarity.name().toLowerCase(Locale.ROOT);
    }

    private String displayNameForRarity(String baseName, SpellDeckRarity rarity) {
        String safeName = baseName == null || baseName.isBlank() ? "Spell" : baseName;
        return switch (rarity == null ? SpellDeckRarity.COMMON : rarity) {
            case COMMON -> safeName;
            case UNCOMMON -> "Refined " + safeName;
            case RARE -> "Greater " + safeName;
            case EPIC -> "Empowered " + safeName;
            case LEGENDARY -> "Legendary " + safeName;
            case MYTHIC -> "Mythic " + safeName;
        };
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
        definitionsBySpellId.putIfAbsent(normalize(definition.spellId()), definition);
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
        Map<String, SpellCardDefinition> bestByFamily = new HashMap<>();
        for (SpellCardDefinition definition : definitions.values()) {
            if (profile.getCopies(definition.cardId()) <= 0) {
                continue;
            }
            String family = normalize(definition.familyId());
            SpellCardDefinition current = bestByFamily.get(family);
            if (current == null || definition.rarity().ordinal() > current.rarity().ordinal()) {
                bestByFamily.put(family, definition);
            }
        }
        List<SpellCardDefinition> owned = new ArrayList<>(bestByFamily.values());
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
        String normalized = normalize(spellId);
        SpellCardDefinition direct = definitionsBySpellId.get(normalized);
        if (direct != null) {
            return direct;
        }
        me.nakilex.levelplugin.spells.progression.SpellProgressionManager progressionManager =
                me.nakilex.levelplugin.spells.progression.SpellProgressionManager.getInstance();
        for (SpellCardDefinition definition : definitions.values()) {
            int maxLevel = progressionManager.getMaxLevel(definition.spellId());
            for (int level = 1; level <= maxLevel; level++) {
                String candidate = progressionManager.getSpellIdAtLevel(definition.spellId(), level);
                if (candidate != null && candidate.equalsIgnoreCase(normalized)) {
                    return definition;
                }
            }
        }
        return null;
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
        int masteryLevel = getMasteryLevel(getProfile(playerId), card);
        int rarityOffset = spellLevelOffset(card.rarity());
        return me.nakilex.levelplugin.spells.progression.SpellProgressionManager.getInstance()
                .getSpellIdAtLevel(card.spellId(), masteryLevel + rarityOffset);
    }

    public String getEffectiveSpellId(UUID playerId, String spellId) {
        SpellCardDefinition card = getDefinitionBySpellId(spellId);
        return card == null ? spellId : getEffectiveSpellId(playerId, card);
    }

    private int spellLevelOffset(SpellDeckRarity rarity) {
        return switch (rarity == null ? SpellDeckRarity.COMMON : rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
            case MYTHIC -> 5;
        };
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

    private SpellCardDefinition getOwnedCardForFamily(SpellDeckProfile profile, String familyId) {
        if (profile == null || familyId == null || familyId.isBlank()) {
            return null;
        }
        SpellCardDefinition best = null;
        String normalizedFamily = normalize(familyId);
        for (SpellCardDefinition definition : definitionsByFamily.getOrDefault(normalizedFamily, List.of())) {
            if (profile.getCopies(definition.cardId()) <= 0) {
                continue;
            }
            if (best == null || definition.rarity().ordinal() > best.rarity().ordinal()) {
                best = definition;
            }
        }
        return best;
    }

    private void replaceOwnedFamilyCard(SpellDeckProfile profile,
                                        SpellCardDefinition previous,
                                        SpellCardDefinition replacement) {
        if (profile == null || replacement == null) {
            return;
        }
        SpellInputType equippedSlot = getEquippedSlotForFamily(profile, replacement.familyId());
        for (SpellCardDefinition definition : definitionsByFamily.getOrDefault(normalize(replacement.familyId()), List.of())) {
            profile.setCopies(definition.cardId(), 0);
        }
        profile.setCopies(replacement.cardId(), 1);
        if (equippedSlot != null) {
            profile.equip(equippedSlot, replacement.cardId());
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
        return pull(player, amount, true);
    }

    public SpellPullResult pull(Player player, int amount, boolean grantAutoDiscardRewards) {
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

            SpellCardDefinition ownedFamilyCard = getOwnedCardForFamily(profile, card.familyId());
            int invested = profile.getInvestedCopies(card.familyId());
            if (ownedFamilyCard == null) {
                profile.addCopies(card.cardId(), 1);
                autoEquipFirstCopy(player, profile, card);
                unlocked.merge(card, 1, Integer::sum);
                entries.add(new SpellPullEntry(card, SpellPullOutcome.UNLOCKED, 0, 0));
            } else if (card.rarity().ordinal() > ownedFamilyCard.rarity().ordinal()) {
                replaceOwnedFamilyCard(profile, ownedFamilyCard, card);
                unlocked.merge(card, 1, Integer::sum);
                entries.add(new SpellPullEntry(card, SpellPullOutcome.UNLOCKED, 0, 0));
            } else if (invested < maxMasteryInvestedCopies()) {
                int masteryValue = Math.min(masteryValue(card.rarity()), maxMasteryInvestedCopies() - invested);
                profile.addInvestedCopies(card.familyId(), masteryValue);
                masteryGained.merge(card, masteryValue, Integer::sum);
                entries.add(new SpellPullEntry(card, SpellPullOutcome.MASTERY_GAINED, masteryValue, 0));
            } else {
                int gems = grantAutoDiscardRewards ? maxedDuplicateGemValue(card.rarity()) : 0;
                if (gems > 0) {
                    addGems(player, gems);
                    salvagedGems += gems;
                }
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
            int availableMastery = Math.max(0, maxMasteryInvestedCopies() - profile.getInvestedCopies(definition.familyId()));
            int investableCopies = Math.min(duplicateCopies, availableMastery);
            int salvageCopies = duplicateCopies - investableCopies;
            profile.setCopies(definition.cardId(), 1);
            if (investableCopies > 0) {
                profile.addInvestedCopies(definition.familyId(), investableCopies);
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

    public int getMaxMasteryLevel() {
        return MAX_MASTERY_LEVEL;
    }

    public int maxMasteryInvestedCopies() {
        return investedCopiesForLevel(MAX_MASTERY_LEVEL);
    }

    public int getMasteryLevel(SpellDeckProfile profile, SpellCardDefinition card) {
        if (profile == null || card == null) {
            return 0;
        }
        return getMasteryLevelForInvested(profile.getInvestedCopies(card.familyId()));
    }

    public int getMasteryLevel(UUID playerId, String spellId) {
        if (dataStore == null || playerId == null || spellId == null) {
            return 0;
        }
        SpellCardDefinition card = getDefinitionBySpellId(spellId);
        if (card == null) {
            return 0;
        }
        return getMasteryLevel(dataStore.getProfile(playerId), card);
    }

    public double getMasteryManaCooldownMultiplier(UUID playerId, String spellId) {
        int masteryLevel = getMasteryLevel(playerId, spellId);
        double reduction = Math.min(0.25, masteryLevel * MASTERY_MANA_COOLDOWN_REDUCTION_PER_LEVEL);
        return Math.max(0.0, 1.0 - reduction);
    }

    public int getMasteryProgress(SpellDeckProfile profile, SpellCardDefinition card) {
        if (profile == null || card == null) {
            return 0;
        }
        int invested = Math.min(profile.getInvestedCopies(card.familyId()), maxMasteryInvestedCopies());
        int masteryLevel = getMasteryLevelForInvested(invested);
        if (masteryLevel >= MAX_MASTERY_LEVEL) {
            return getMasteryRequiredForNextLevel(masteryLevel);
        }
        return invested - investedCopiesForLevel(masteryLevel);
    }

    public int getMasteryRequiredForNextLevel(int masteryLevel) {
        if (masteryLevel >= MAX_MASTERY_LEVEL) {
            return 0;
        }
        return masteryLevel + 1;
    }

    public int maxedDuplicateGemValue(SpellDeckRarity rarity) {
        return MAXED_DUPLICATE_GEMS.getOrDefault(rarity == null ? SpellDeckRarity.COMMON : rarity, 2);
    }

    public int masteryValue(SpellDeckRarity rarity) {
        return MASTERY_VALUE_BY_RARITY.getOrDefault(rarity == null ? SpellDeckRarity.COMMON : rarity, 1);
    }

    private int getMasteryLevelForInvested(int investedCopies) {
        int level = 0;
        int safeInvested = Math.max(0, investedCopies);
        while (level < MAX_MASTERY_LEVEL && safeInvested >= investedCopiesForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    private int investedCopiesForLevel(int level) {
        int safeLevel = Math.max(0, Math.min(MAX_MASTERY_LEVEL, level));
        return safeLevel * (safeLevel + 1) / 2;
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

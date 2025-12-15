package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import org.bukkit.Material;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import me.nakilex.levelplugin.spells.utils.MythicSkillConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SpellManager {
    private static SpellManager instance;

    private final EffectRegistry effectRegistry;
    private final Map<String, Map<String, Spell>> spellsByClass = new HashMap<>();
    private final Map<String, Map<String, Spell>> spellsById = new HashMap<>();
    private Main plugin;

    private static final List<Material> WARRIOR_WEAPONS = new ArrayList<>();
    static {
        WARRIOR_WEAPONS.addAll(WeaponType.SWORD.getMaterials());
        WARRIOR_WEAPONS.addAll(WeaponType.AXE.getMaterials());
        WARRIOR_WEAPONS.addAll(WeaponType.SHOVEL.getMaterials());
    }

    // Standardized level unlock thresholds
    private static final int LVL_ONE   = 1;
    private static final int LVL_THREE = 3;
    private static final int LVL_FIVE  = 5;
    private static final int LVL_EIGHT = 8;
    private static final int LVL_TEN   = 10;

    public static SpellManager getInstance() {
        if (instance == null) throw new IllegalStateException("SpellManager not init’d!");
        return instance;
    }

    /**
     * Convenience constructor for demo utilities that don’t have access to the
     * plugin instance. This simply delegates to the main constructor using the
     * singleton {@link Main} instance.
     */
    public SpellManager() {
        this(Main.getInstance());
    }

    public SpellManager(Main plugin) {
        instance = this;
        this.plugin = plugin;
        this.effectRegistry = EffectRegistry.getInstance();
        loadSpells();
    }

    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }

    /**
     * Lookup a spell by its id rather than combo.
     */
    public Spell getSpellById(String className, String id) {
        Map<String, Spell> classMap = spellsById.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(id.toLowerCase());
    }

    public Map<String, Spell> getSpellsByClass(String className) {
        return spellsByClass.getOrDefault(className.toLowerCase(), Collections.emptyMap());
    }


    /**
     * Builds a basic attack spell that relies on Technique-based cooldowns rather than
     * MythicMobs' own cooldown settings.
     */
    private static Spell basicAttack(String id, String name, List<Material> weapons, String effectKey) {
        return basicAttack(id, name, weapons, effectKey, 0L);
    }

    private static Spell basicAttack(String id, String name, List<Material> weapons, String effectKey, long cooldownSeconds) {
        return new Spell(id, name, "BASIC_ATTACK", 0.0, cooldownSeconds, LVL_ONE, weapons, effectKey, 0.0);
    }

    private void loadSpells() {

        // — ARCHER CLASS —
        Map<String, Spell> archerMap = new HashMap<>();
        archerMap.put("BASIC_ATTACK", basicAttack(
            "quick_shot",
            "Quick_Shot",
            WeaponType.BOW.getMaterials(),
            "MYTHIC_QUICK_SHOT",
            1L
        ));
        archerMap.put("LEFT", new Spell(
            "backstep", "Backstep", "LEFT",
            5.0,
            Math.max(1L, MythicSkillConfig.getCooldownSeconds("Backstep")), LVL_THREE,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_BACKSTEP", 0.0,
            false,
            true
        ));
        archerMap.put("SNEAK", new Spell(
            "arrow_barrage", "Arrow Barrage", "SNEAK",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Arrow_Barrage"), LVL_FIVE,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_ARROW_BARRAGE", 0.0
        ));
        archerMap.put("SHIFT_LEFT", new Spell(
            "bow_drone", "Bow Drone", "SHIFT_LEFT",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Deadly_Javelin"), LVL_EIGHT,
            WeaponType.BOW.getMaterials(),
            "BOW_DRONE", 0.0
        ));
        archerMap.put("SHIFT_RIGHT", new Spell(
            "dragon_piercer", "Dragon Piercer", "SHIFT_RIGHT",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Piercer"), LVL_TEN,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_DRAGON_PIERCER", 0.0
        ));
        spellsByClass.put("archer", Collections.unmodifiableMap(archerMap));
        Map<String, Spell> archerIdMap = new HashMap<>();
        for (Spell s : archerMap.values()) archerIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("archer", Collections.unmodifiableMap(archerIdMap));
        plugin.getLogger().info("[SPELLS] Archer combos: " + archerMap.keySet());

        // — PHOENIXHUNTER CLASS —
        Map<String, Spell> phoenixMap = new HashMap<>();
        phoenixMap.put("BASIC_ATTACK", basicAttack(
            "blazing_feathers",
            "Blazing Feathers",
            WeaponType.BOW.getMaterials(),
            "MYTHIC_BLAZING_FEATHERS"
        ));
        phoenixMap.put("LRL", new Spell(
            "ashdance", "Ashdance", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Ashdance"), 3,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_ASHDANCE", 0.0
        ));
        phoenixMap.put("LRR", new Spell(
            "flameburst_convergence", "Flameburst Convergence", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Flameburst_Convergence"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_FLAMEBURST_CONVERGENCE", 0.0
        ));
        phoenixMap.put("LLR", new Spell(
            "phoenix_totem", "Phoenix Totem", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Phoenix_Totem"), 0,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PHOENIX_TOTEM", 0.0,
            true,
            false
        ));
        phoenixMap.put("LLL", new Spell(
            "pyroclasmic_barrage", "Pyroclasmic Barrage", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Pyroclasmic_Barrage"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PYROCLASMIC_BARRAGE", 0.0
        ));
        phoenixMap.put("RRR", new Spell(
            "phoenix_rebirth", "Phoenix Rebirth", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Phoenix_Rebirth"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PHOENIX_REBIRTH", 0.0
        ));
        spellsByClass.put("phoenixhunter", Collections.unmodifiableMap(phoenixMap));
        Map<String, Spell> phoenixIdMap = new HashMap<>();
        for (Spell s : phoenixMap.values()) phoenixIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("phoenixhunter", Collections.unmodifiableMap(phoenixIdMap));
        plugin.getLogger().info("[SPELLS] PhoenixHunter combos: " + phoenixMap.keySet());

        // — CLERIC CLASS —
        Map<String, Spell> clericMap = new HashMap<>();
        spellsByClass.put("cleric", Collections.unmodifiableMap(clericMap));
        spellsById.put("cleric", Collections.emptyMap());
        plugin.getLogger().info("[SPELLS] Cleric combos: " + clericMap.keySet());

        // — DEADEYE CLASS —
        Map<String, Spell> deadeyeMap = new HashMap<>();
        deadeyeMap.put("BASIC_ATTACK", basicAttack(
            "pistol_shot", "Pistol Shot",
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PISTOL_SHOT"
        ));
        deadeyeMap.put("LRL", new Spell(
            "shotgun_blast", "Shotgun Blast", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Shotgun_Blast"), 3,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_SHOTGUN_BLAST", 0.0
        ));
        deadeyeMap.put("LRR", new Spell(
            "sniper_backup", "Sniper Backup", "LRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Sniper_Backup"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_SNIPER_BACKUP", 0.0
        ));
        deadeyeMap.put("LLR", new Spell(
            "deathfire", "Deathfire", "LLR",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Deathfire"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_DEATHFIRE", 0.0
        ));
        deadeyeMap.put("LLL", new Spell(
            "focus_shot", "Focus Shot", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Focus_Shot"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_FOCUS_SHOT", 0.0
        ));
        deadeyeMap.put("RRR", new Spell(
            "air_strike", "Air Strike", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Air_Strike"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_AIR_STRIKE", 0.0
        ));
        spellsByClass.put("deadeye", Collections.unmodifiableMap(deadeyeMap));
        Map<String, Spell> deadeyeIdMap = new HashMap<>();
        for (Spell s : deadeyeMap.values()) deadeyeIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("deadeye", Collections.unmodifiableMap(deadeyeIdMap));
        plugin.getLogger().info("[SPELLS] Deadeye combos: " + deadeyeMap.keySet());

        // — ROGUE CLASS —
        Map<String, Spell> rogueMap = new HashMap<>();
        rogueMap.put("BASIC_ATTACK", basicAttack(
            "blade_slash", "Blade Slash",
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BLADE_SLASH"
        ));
        rogueMap.put("LRL", new Spell(
            "assassin_dash", "Assassin Dash", "LRL",
            5.0,
            MythicSkillConfig.getCooldownSeconds("Assassin_Dash"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_ASSASSIN_DASH", 0.0,
            false,
            true
        ));
        rogueMap.put("LLL", new Spell(
            "dagger_throw", "Dagger Throw", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Dagger_Throw"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_DAGGER_THROW", 0.0
        ));
        rogueMap.put("RRR", new Spell(
            "blade_dance", "Blade Dance", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Blade_Dance"), 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BLADE_DANCE", 0.0
        ));
        rogueMap.put("LRR", new Spell(
            "shadow_walk", "Shadow Walk", "LRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Shadow_Walk_Skill"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_SHADOW_WALK", 0.0,
            false,
            false
        ));
        spellsByClass.put("rogue", Collections.unmodifiableMap(rogueMap));
        Map<String, Spell> rogueIdMap = new HashMap<>();
        for (Spell s : rogueMap.values()) rogueIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("rogue", Collections.unmodifiableMap(rogueIdMap));
        plugin.getLogger().info("[SPELLS] Rogue combos: " + rogueMap.keySet());

        // — AWAKROGUE CLASS —
        Map<String, Spell> awakrogueMap = new HashMap<>();
        awakrogueMap.put("BASIC_ATTACK", basicAttack(
            "lethal_combo", "Lethal Combo",
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_LETHAL_COMBO"
        ));
        awakrogueMap.put("LRL", new Spell(
            "ravaging_dash", "Ravaging Dash", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Ravaging_Dash_CAST"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_RAVAGING_DASH", 0.0
        ));
        awakrogueMap.put("LLL", new Spell(
            "crimson_arc", "Crimson Arc", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Crimson_Arc"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_CRIMSON_ARC", 0.0
        ));
        awakrogueMap.put("RRR", new Spell(
            "last_dance", "Last Dance", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Last_Dance"), 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_LAST_DANCE", 0.0
        ));
        awakrogueMap.put("LLR", new Spell(
            "shadowquake", "Shadowquake", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Shadowquake_Skill"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_SHADOWQUAKE", 0.0
        ));
        // Death Bloom is triggered via the player sneaking rather than a click combo
        awakrogueMap.put("SNEAK", new Spell(
            "death_bloom", "Death Bloom", "SNEAK",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Death_Bloom_CAST"), 8,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_DEATH_BLOOM", 0.0
        ));
        awakrogueMap.put("LRR", new Spell(
            "deadly_calm", "Deadly Calm", "LRR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Deadly_Calm"), 0,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_DEADLY_CALM", 0.0,
            true,
            false
        ));
        spellsByClass.put("awakrogue", Collections.unmodifiableMap(awakrogueMap));
        Map<String, Spell> awakRogueIdMap = new HashMap<>();
        for (Spell s : awakrogueMap.values()) awakRogueIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("awakrogue", Collections.unmodifiableMap(awakRogueIdMap));
        plugin.getLogger().info("[SPELLS] AwakRogue combos: " + awakrogueMap.keySet());

        // — AWAKWARRIOR CLASS —
        Map<String, Spell> awakwarriorMap = new HashMap<>();
        awakwarriorMap.put("BASIC_ATTACK", basicAttack(
            "brutal_combo", "Brutal Combo",
            WARRIOR_WEAPONS,
            "MYTHIC_BRUTAL_COMBO"
        ));
        long berserkCooldown = Math.max(2L, MythicSkillConfig.getCooldownSeconds("Berserkers_Leap"));
        awakwarriorMap.put("LRL", new Spell(
            "berserkers_leap", "Berserkers Leap", "LRL",
            8.0,
            berserkCooldown, 3,
            WARRIOR_WEAPONS,
            "MYTHIC_BERSERKERS_LEAP", 0.0
        ));
        awakwarriorMap.put("LLR", new Spell(
            "bloodbound_barrier", "Bloodbound Barrier", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Bloodbound_Barrier"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_BLOODBOUND_BARRIER", 0.0
        ));
        awakwarriorMap.put("LLL", new Spell(
            "relentless_whirlwind", "Relentless Whirlwind", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Relentless_Whirlwind"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_RELENTLESS_WHIRLWIND", 0.0
        ));
        awakwarriorMap.put("RLL", new Spell(
            "vicious_strike", "Vicious Strike", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Vicious_Strike"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_VICIOUS_STRIKE", 0.0
        ));
        awakwarriorMap.put("RRR", new Spell(
            "strike_of_fury", "Strike Of Fury", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Strike_Of_Fury"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_STRIKE_OF_FURY", 0.0
        ));
        awakwarriorMap.put("LRR", new Spell(
            "bulwark_instinct", "Bulwark Instinct", "LRR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Bulwark_Instinct"), 0,
            WARRIOR_WEAPONS,
            "MYTHIC_BULWARK_INSTINCT", 0.0,
            true,
            false
        ));
        spellsByClass.put("awakwarrior", Collections.unmodifiableMap(awakwarriorMap));
        Map<String, Spell> awakIdMap = new HashMap<>();
        for (Spell s : awakwarriorMap.values()) awakIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("awakwarrior", Collections.unmodifiableMap(awakIdMap));
        plugin.getLogger().info("[SPELLS] AwakWarrior combos: " + awakwarriorMap.keySet());

        // — AWAKARCHER CLASS —
        Map<String, Spell> awakarcherMap = new HashMap<>();
        awakarcherMap.put("BASIC_ATTACK", basicAttack(
            "blasting_combo", "Blasting Combo",
            WeaponType.BOW.getMaterials(),
            "MYTHIC_BLASTING_COMBO"
        ));
        awakarcherMap.put("LRL", new Spell(
            "evasive_shot", "Evasive Shot", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Evasive_Shot_CAST"), 3,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_EVASIVE_SHOT", 0.0
        ));
        awakarcherMap.put("LLL", new Spell(
            "piercing_skyfall", "Piercing Skyfall", "LLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Piercing_Skyfall_Skill"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_PIERCING_SKYFALL", 0.0
        ));
        awakarcherMap.put("RLL", new Spell(
            "rapid_arrows", "Rapid Arrows", "RLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Rapid_Arrows_CAST"), 8,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_RAPID_ARROWS", 0.0
        ));
        awakarcherMap.put("RRR", new Spell(
            "shot_of_destruction", "Shot Of Destruction", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Shot_Of_Destruction_CAST"), 10,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_SHOT_OF_DESTRUCTION", 0.0
        ));
        awakarcherMap.put("SNEAK", new Spell(
            "volley_of_arrows", "Volley Of Arrows", "SNEAK",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Volley_Of_Arrows_SKILL"), 5,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_VOLLEY_OF_ARROWS", 0.0
        ));
        awakarcherMap.put("LRR", new Spell(
            "ambush", "Ambush", "LRR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Ambush"), 0,
            WeaponType.BOW.getMaterials(),
            "MYTHIC_AMBUSH", 0.0,
            true,
            false
        ));
        spellsByClass.put("awakarcher", Collections.unmodifiableMap(awakarcherMap));
        Map<String, Spell> awakArIdMap = new HashMap<>();
        for (Spell s : awakarcherMap.values()) awakArIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("awakarcher", Collections.unmodifiableMap(awakArIdMap));
        plugin.getLogger().info("[SPELLS] AwakArcher combos: " + awakarcherMap.keySet());

        // — AWAKMAGE CLASS —
        Map<String, Spell> awakmageMap = new HashMap<>();
        awakmageMap.put("BASIC_ATTACK", basicAttack(
            "sorcery_combo", "Sorcery Combo",
            WeaponType.WAND.getMaterials(),
            "MYTHIC_SORCERY_COMBO"
        ));
        awakmageMap.put("LRL", new Spell(
            "teleport_strike", "Teleport Strike", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Teleport_Strike_CAST"), LVL_THREE,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_TELEPORT_STRIKE", 0.0,
            false,
            true
        ));
        awakmageMap.put("LLL", new Spell(
            "blazing_barrage", "Blazing Barrage", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Blazing_Barrage_CAST"), LVL_EIGHT,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_BLAZING_BARRAGE", 0.0
        ));
        awakmageMap.put("LLR", new Spell(
            "cryo_prison", "Cryo Prison", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Cryo_Prison_Skill"), LVL_FIVE,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_CRYO_PRISON", 0.0
        ));
        awakmageMap.put("RLL", new Spell(
            "hailpiercer", "Hailpiercer", "RLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Hailpiercer"), LVL_FIVE,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_HAILPIERCER", 0.0
        ));
        awakmageMap.put("RRR", new Spell(
            "meteor_of_doom", "Meteor Of Doom", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Meteor_Of_Doom"), LVL_TEN,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_METEOR_OF_DOOM", 0.0
        ));
        awakmageMap.put("SNEAK", new Spell(
            "mana_barrier", "Mana Barrier", "SNEAK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Mana_Barrier"), LVL_THREE,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_MANA_BARRIER", 0.0
        ));
        spellsByClass.put("awakmage", Collections.unmodifiableMap(awakmageMap));
        Map<String, Spell> awakMageIdMap = new HashMap<>();
        for (Spell s : awakmageMap.values()) awakMageIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("awakmage", Collections.unmodifiableMap(awakMageIdMap));
        plugin.getLogger().info("[SPELLS] AwakMage combos: " + awakmageMap.keySet());

        // — ARCHMAGE CLASS —
        Map<String, Spell> archmageMap = new HashMap<>();
        archmageMap.put("BASIC_ATTACK", basicAttack(
            "arcane_slash", "Arcane Slash",
            WeaponType.WAND.getMaterials(),
            "MYTHIC_ARCANE_SLASH"
        ));
        archmageMap.put("LRL", new Spell(
            "blizzard", "Blizzard", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Blizzard"), 3,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_BLIZZARD", 0.0
        ));
        archmageMap.put("LLL", new Spell(
            "chains_of_void", "Chains Of Void", "LLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Chains_Of_Void"), 5,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_CHAINS_OF_VOID", 0.0
        ));
        archmageMap.put("RLL", new Spell(
            "cloak_of_hastur", "Cloak Of Hastur", "RLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Cloak_Of_Hastur_SKILL"), 5,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_CLOAK_OF_HASTUR", 0.0
        ));
        archmageMap.put("RRR", new Spell(
            "arcane_devastation", "Arcane Devastation", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Arcane_Devastation"), 10,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_ARCANE_DEVASTATION", 0.0
        ));
        archmageMap.put("SNEAK", new Spell(
            "meteor_storm", "Meteor Storm", "SNEAK",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Meteor_Storm_Skill"), 5,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_METEOR_STORM", 0.0
        ));
        archmageMap.put("LRR", new Spell(
            "arcane_shield", "Arcane Shield", "LRR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Arcane_Shield_Damaged_BLUE"), 0,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_ARCANE_SHIELD", 0.0,
            true,
            false
        ));
        spellsByClass.put("archmage", Collections.unmodifiableMap(archmageMap));
        Map<String, Spell> archmageIdMap = new HashMap<>();
        for (Spell s : archmageMap.values()) archmageIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("archmage", Collections.unmodifiableMap(archmageIdMap));
        plugin.getLogger().info("[SPELLS] Archmage combos: " + archmageMap.keySet());

        // — WARRIOR CLASS —
        Map<String, Spell> warriorMap = new HashMap<>();
        warriorMap.put("BASIC_ATTACK", basicAttack(
            "brutal_strike", "Brutal Strike",
            WARRIOR_WEAPONS,
            "MYTHIC_BRUTAL_STRIKE"
        ));
        warriorMap.put("LRL", new Spell(
            "charge", "Charge", "LRL",
            5.0,
            MythicSkillConfig.getCooldownSeconds("Charge"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_CHARGE", 0.0,
            false,
            true
        ));
        warriorMap.put("LRR", new Spell(
            "chain_hook", "Vortex Pull", "LRR",
            6.0,
            5L,
            5,
            WARRIOR_WEAPONS,
            "VORTEX_PULL", 0.0
        ));
        warriorMap.put("LLR", new Spell(
            "shield_barrier", "Shield Barrier", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Shield_Barrier"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_SHIELD_BARRIER", 0.0
        ));
        warriorMap.put("RLL", new Spell(
            "shockwave", "Shockwave", "RLL",
            14.0,
            MythicSkillConfig.getCooldownSeconds("Whirlwind"), 3,
            WARRIOR_WEAPONS,
            "SHOCKWAVE", 1.5
        ));
        warriorMap.put("LLL", new Spell(
            "judgement", "Judgement", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Judgement"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_JUDGEMENT", 0.0
        ));
        warriorMap.put("RRR", new Spell(
            "rampage", "Rampage", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Rampage"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_RAMPAGE", 0.0
        ));
        spellsByClass.put("warrior", Collections.unmodifiableMap(warriorMap));
        Map<String, Spell> warriorIdMap = new HashMap<>();
        for (Spell s : warriorMap.values()) warriorIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("warrior", Collections.unmodifiableMap(warriorIdMap));
        plugin.getLogger().info("[SPELLS] Warrior combos: " + warriorMap.keySet());

        // — BARBARIAN CLASS —
        Map<String, Spell> barbarianMap = new HashMap<>();
        barbarianMap.put("BASIC_ATTACK", basicAttack(
            "rageblade", "Rageblade",
            WARRIOR_WEAPONS,
            "MYTHIC_RAGEBLADE"
        ));
        barbarianMap.put("LRL", new Spell(
            "primal_axe", "Primal Axe", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Primal_Axe"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_PRIMAL_AXE", 0.0
        ));
        barbarianMap.put("LLR", new Spell(
            "war_cry", "War Cry", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("War_Cry"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_WAR_CRY", 0.0
        ));
        barbarianMap.put("LLL", new Spell(
            "double_edge", "Double Edge", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Double_Edge"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DOUBLE_EDGE", 0.0
        ));
        barbarianMap.put("RLL", new Spell(
            "relentless_leap", "Relentless Leap", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Relentless_Leap"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_RELENTLESS_LEAP", 0.0
        ));
        barbarianMap.put("RRR", new Spell(
            "eternal_fury", "Eternal Fury", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Eternal_Fury"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_ETERNAL_FURY", 0.0
        ));
        spellsByClass.put("barbarian", Collections.unmodifiableMap(barbarianMap));
        Map<String, Spell> barbarianIdMap = new HashMap<>();
        for (Spell s : barbarianMap.values()) barbarianIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("barbarian", Collections.unmodifiableMap(barbarianIdMap));
        plugin.getLogger().info("[SPELLS] Barbarian combos: " + barbarianMap.keySet());

        // — PALADIN CLASS —
        Map<String, Spell> paladinMap = new HashMap<>();
        paladinMap.put("BASIC_ATTACK", basicAttack(
            "holy_strike", "Holy Strike",
            WARRIOR_WEAPONS,
            "MYTHIC_HOLY_STRIKE"
        ));
        paladinMap.put("LRL", new Spell(
            "bound_seal", "Bound Seal", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Bound_Seal"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_BOUND_SEAL", 0.0
        ));
        paladinMap.put("LRR", new Spell(
            "hammer_of_justice", "Hammer Of Justice", "LRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Hammer_Of_Justice"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_HAMMER_OF_JUSTICE", 0.0
        ));
        paladinMap.put("LLL", new Spell(
            "heavenly_shield", "Heavenly Shield", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Heavenly_Shield"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_HEAVENLY_SHIELD", 0.0
        ));
        paladinMap.put("RLL", new Spell(
            "unbreakable_will", "Unbreakable Will", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Unbreakable_Will"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_UNBREAKABLE_WILL", 0.0
        ));
        paladinMap.put("RRR", new Spell(
            "last_stand", "Last Stand", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Last_Stand"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_LAST_STAND", 0.0
        ));
        spellsByClass.put("paladin", Collections.unmodifiableMap(paladinMap));
        Map<String, Spell> paladinIdMap = new HashMap<>();
        for (Spell s : paladinMap.values()) paladinIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("paladin", Collections.unmodifiableMap(paladinIdMap));
        plugin.getLogger().info("[SPELLS] Paladin combos: " + paladinMap.keySet());

        // — DEATH KNIGHT CLASS —
        Map<String, Spell> deathMap = new HashMap<>();
        deathMap.put("BASIC_ATTACK", basicAttack(
            "death_strike", "Death Strike",
            WARRIOR_WEAPONS,
            "MYTHIC_DEATH_STRIKE"
        ));
        deathMap.put("LRL", new Spell(
            "phantom_charge", "Phantom Charge", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Phantom_Charge"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_PHANTOM_CHARGE", 0.0
        ));
        deathMap.put("LRR", new Spell(
            "wraithbound_chains", "Wraithbound Chains", "LRR",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Wraithbound_Chains"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_WRAITHBOUND_CHAINS", 0.0
        ));
        deathMap.put("LLR", new Spell(
            "soul_barrier", "Soul Barrier", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Soul_Barrier"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_SOUL_BARRIER", 0.0
        ));
        deathMap.put("LLL", new Spell(
            "necrotic_whirlwind", "Necrotic Whirlwind", "LLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Necrotic_Whirlwind"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_NECROTIC_WHIRLWIND", 0.0
        ));
        deathMap.put("RRR", new Spell(
            "death_sentence", "Death Sentence", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Death_Sentence"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_DEATH_SENTENCE", 0.0
        ));
        spellsByClass.put("deathknight", Collections.unmodifiableMap(deathMap));
        Map<String, Spell> deathIdMap = new HashMap<>();
        for (Spell s : deathMap.values()) deathIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("deathknight", Collections.unmodifiableMap(deathIdMap));
        plugin.getLogger().info("[SPELLS] DeathKnight combos: " + deathMap.keySet());

        // — ABYSSION CLASS —
        Map<String, Spell> abyssionMap = new HashMap<>();
        abyssionMap.put("BASIC_ATTACK", basicAttack(
            "aqua_slash", "Aqua Slash",
            WARRIOR_WEAPONS,
            "MYTHIC_AQUA_SLASH"
        ));
        abyssionMap.put("LRL", new Spell(
            "abyssal_dash", "Abyssal Dash", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Abyssal_Dash"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_ABYSSAL_DASH", 0.0
        ));
        abyssionMap.put("LLL", new Spell(
            "tidal_wave", "Tidal Wave", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Tidal_Wave"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_TIDAL_WAVE", 0.0
        ));
        abyssionMap.put("RLL", new Spell(
            "aqua_aura", "Aqua Aura", "RLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Aqua_Aura"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_AQUA_AURA", 0.0
        ));
        abyssionMap.put("RRR", new Spell(
            "abyssal_smash", "Abyssal Smash", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Abyssal_Smash"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_ABYSSAL_SMASH", 0.0
        ));
        spellsByClass.put("abyssion", Collections.unmodifiableMap(abyssionMap));
        Map<String, Spell> abyssionIdMap = new HashMap<>();
        for (Spell s : abyssionMap.values()) abyssionIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("abyssion", Collections.unmodifiableMap(abyssionIdMap));
        plugin.getLogger().info("[SPELLS] Abyssion combos: " + abyssionMap.keySet());

        // — MAGE CLASS —
        Map<String, Spell> mageMap = new HashMap<>();
        mageMap.put("BASIC_ATTACK", basicAttack(
            "fireball", "Fireball",
            WeaponType.WAND.getMaterials(),
            "MYTHIC_FIREBALL"
        ));
        mageMap.put("LRL", new Spell(
            "blink", "Blink", "LRL",
            8.0,
            1,
            3,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_BLINK", 0.0,
            false,
            true
        ));
        mageMap.put("LLL", new Spell(
            "meteor", "Meteor", "LLL",
            12.0,
            2,
            5,
            WeaponType.WAND.getMaterials(),
            "MAGE_MAGMA_METEOR", 10.0
        ));
        mageMap.put("LRR", new Spell(
            "frost_nova", "Frost Nova", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Frost_Nova"), 5,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_FROST_NOVA", 0.0
        ));
        mageMap.put("RRR", new Spell(
            "inferno_chains", "Inferno Chains", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Inferno_Chains"), 10,
            WeaponType.WAND.getMaterials(),
            "MYTHIC_INFERNO_CHAINS", 0.0
        ));
        spellsByClass.put("mage", Collections.unmodifiableMap(mageMap));
        Map<String, Spell> mageIdMap = new HashMap<>();
        for (Spell s : mageMap.values()) mageIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("mage", Collections.unmodifiableMap(mageIdMap));
        plugin.getLogger().info("[SPELLS] Mage combos: " + mageMap.keySet());

        // — DRAGONIAN CLASS —
        Map<String, Spell> dragonianMap = new HashMap<>();
        dragonianMap.put("BASIC_ATTACK", basicAttack(
            "dragonian_slash", "Dragonian Slash",
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_L_T"
        ));
        dragonianMap.put("LRL", new Spell(
            "dragonian_lunge", "Dragonian Lunge", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_r_t"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_R_T", 0.0
        ));
        dragonianMap.put("LLL", new Spell(
            "dragonian_rs", "Dragonian RS", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_rs_t"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_RS_T", 0.0
        ));
        dragonianMap.put("LLR", new Spell(
            "dragonian_ss", "Dragonian Stance", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_ss_t"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_SS_T", 0.0
        ));
        dragonianMap.put("RRR", new Spell(
            "taotie_dragon", "Taotie Dragon", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("dragonian_ls_t"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONIAN_LS_T", 0.0
        ));
        spellsByClass.put("dragonian", Collections.unmodifiableMap(dragonianMap));
        Map<String, Spell> dragonianIdMap = new HashMap<>();
        for (Spell s : dragonianMap.values()) dragonianIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("dragonian", Collections.unmodifiableMap(dragonianIdMap));
        plugin.getLogger().info("[SPELLS] Dragonian combos: " + dragonianMap.keySet());

        // — DRAGON WARRIOR CLASS —
        Map<String, Spell> dragonwarriorMap = new HashMap<>();
        dragonwarriorMap.put("BASIC_ATTACK", basicAttack(
            "dragon_slash", "Dragon Slash",
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_SLASH"
        ));
        dragonwarriorMap.put("LRL", new Spell(
            "dragon_dash", "Dragon Dash", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Dash"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_DASH", 0.0
        ));
        dragonwarriorMap.put("LLL", new Spell(
            "dragon_breath", "Dragon Breath", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Breath"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_BREATH", 0.0
        ));
        dragonwarriorMap.put("LLR", new Spell(
            "dragon_zone", "Dragon Zone", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Zone"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGON_ZONE", 0.0
        ));
        dragonwarriorMap.put("RRR", new Spell(
            "dragonborn", "Dragonborn", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Dragonborn"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_DRAGONBORN", 0.0
        ));
        spellsByClass.put("dragonwarrior", Collections.unmodifiableMap(dragonwarriorMap));
        Map<String, Spell> dragonwarriorIdMap = new HashMap<>();
        for (Spell s : dragonwarriorMap.values()) dragonwarriorIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("dragonwarrior", Collections.unmodifiableMap(dragonwarriorIdMap));
        plugin.getLogger().info("[SPELLS] DragonWarrior combos: " + dragonwarriorMap.keySet());

        // — WINDRUNE CLASS —
        Map<String, Spell> windruneMap = new HashMap<>();
        windruneMap.put("BASIC_ATTACK", basicAttack(
            "gale_slash", "Gale Slash",
            WARRIOR_WEAPONS,
            "MYTHIC_GALE_SLASH"
        ));
        windruneMap.put("LRL", new Spell(
            "vault", "Vault", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Vault"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_VAULT", 0.0
        ));
        windruneMap.put("LLL", new Spell(
            "dancing_blade", "Dancing Blade", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Dancing_Blade"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_DANCING_BLADE", 0.0
        ));
        windruneMap.put("LRR", new Spell(
            "cloudpiercer", "Cloudpiercer", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Cloudpiercer"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_CLOUDPIERCER", 0.0
        ));
        windruneMap.put("RLL", new Spell(
            "torrent", "Torrent", "RLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Torrent"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_TORRENT", 0.0
        ));
        windruneMap.put("RRR", new Spell(
            "windbound_fury", "Windbound Fury", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Windbound_Fury"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_WINDBOUND_FURY", 0.0
        ));
        spellsByClass.put("windrune", Collections.unmodifiableMap(windruneMap));
        Map<String, Spell> windruneIdMap = new HashMap<>();
        for (Spell s : windruneMap.values()) windruneIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("windrune", Collections.unmodifiableMap(windruneIdMap));
        plugin.getLogger().info("[SPELLS] Windrune combos: " + windruneMap.keySet());

        // — ARCTIC KNIGHT CLASS —
        Map<String, Spell> arcticMap = new HashMap<>();
        arcticMap.put("BASIC_ATTACK", basicAttack(
            "frost_strike", "Frost Strike",
            WARRIOR_WEAPONS,
            "MYTHIC_FROST_STRIKE"
        ));
        arcticMap.put("LRL", new Spell(
            "glacial_impalement", "Glacial Impalement", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Glacial_Impalement"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_GLACIAL_IMPALEMENT", 0.0
        ));
        arcticMap.put("LLL", new Spell(
            "frozen_shield", "Frozen Shield", "LLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Frozen_Shield"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_FROZEN_SHIELD", 0.0
        ));
        arcticMap.put("LLR", new Spell(
            "arctic_charge", "Arctic Charge", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Arctic_Charge"), 5,
            WARRIOR_WEAPONS,
            "MYTHIC_ARCTIC_CHARGE", 0.0
        ));
        arcticMap.put("RLL", new Spell(
            "glacier_smash", "Glacier Smash", "RLL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Glacier_Smash"), 3,
            WARRIOR_WEAPONS,
            "MYTHIC_GLACIER_SMASH", 0.0
        ));
        arcticMap.put("RRR", new Spell(
            "permafrost_lance", "Permafrost Lance", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Permafrost_Lance"), 10,
            WARRIOR_WEAPONS,
            "MYTHIC_PERMAFROST_LANCE", 0.0
        ));
        spellsByClass.put("arctic", Collections.unmodifiableMap(arcticMap));
        Map<String, Spell> arcticIdMap = new HashMap<>();
        for (Spell s : arcticMap.values()) arcticIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("arctic", Collections.unmodifiableMap(arcticIdMap));
        plugin.getLogger().info("[SPELLS] Arctic combos: " + arcticMap.keySet());

        // — WITCH CLASS —
        Map<String, Spell> witchMap = new HashMap<>();
        witchMap.put("mf_class_witch_normalattack", new Spell(
                "mf_class_witch_normalattack", "Witch Strike", "LEFT",
                0.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_witch_normalattack"), 1,
                WeaponType.SWORD.getMaterials(),
                "mf_class_witch_normalattack", 0.0
        ));
        witchMap.put("mf_class_witch_rightclick", new Spell(
                "mf_class_witch_rightclick", "Crucible Hurl", "RIGHT",
                6.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_witch_rightclick"), 3,
                WeaponType.SWORD.getMaterials(),
                "mf_class_witch_rightclick", 0.0
        ));
        witchMap.put("mf_class_witch_sneak_leftclick", new Spell(
                "mf_class_witch_sneak_leftclick", "Witch Sweep", "LEFT_SNEAK",
                8.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_witch_sneak_leftclick"), 5,
                WeaponType.SWORD.getMaterials(),
                "mf_class_witch_sneak_leftclick", 0.0
        ));
        witchMap.put("mf_class_witch_sneak_rightclick", new Spell(
                "mf_class_witch_sneak_rightclick", "Witch Cauldron", "RIGHT_SNEAK",
                10.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_witch_sneak_rightclick"), 5,
                WeaponType.SWORD.getMaterials(),
                "mf_class_witch_sneak_rightclick", 0.0
        ));
        witchMap.put("mf_class_witch_shiftshift", new Spell(
                "mf_class_witch_shiftshift", "Witch Dash", "DOUBLE_SNEAK",
                8.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_witch_shiftshift"), 4,
                WeaponType.SWORD.getMaterials(),
                "mf_class_witch_shiftshift", 0.0
        ));
        witchMap.put("mf_class_witch_holdshift", new Spell(
                "mf_class_witch_holdshift", "Witch Ritual", "HOLD_SNEAK",
                20.0,
                MythicSkillConfig.getCooldownSeconds("mf_class_witch_holdshift"), 10,
                WeaponType.SWORD.getMaterials(),
                "mf_class_witch_holdshift", 0.0
        ));
        spellsByClass.put("witch", Collections.unmodifiableMap(witchMap));
        Map<String, Spell> witchIdMap = new HashMap<>();
        for (Spell s : witchMap.values()) witchIdMap.put(s.getId().toLowerCase(), s);
        spellsById.put("witch", Collections.unmodifiableMap(witchIdMap));
        plugin.getLogger().info("[SPELLS] Witch combos: " + witchMap.keySet());


    }

}
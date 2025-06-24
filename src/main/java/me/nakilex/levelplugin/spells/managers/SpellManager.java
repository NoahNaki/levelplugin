package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import me.nakilex.levelplugin.runes.model.RuneEffect;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import me.nakilex.levelplugin.spells.utils.MythicSkillConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SpellManager {
    private static SpellManager instance;

    private final RunesManager runesManager;               // ← add this
    private final EffectRegistry effectRegistry;
    private final Map<String, Map<String, Spell>> spellsByClass = new HashMap<>();
    private Main plugin;

    public static SpellManager getInstance() {
        if (instance == null) throw new IllegalStateException("SpellManager not init’d!");
        return instance;
    }

    public SpellManager(Main plugin, RunesManager runesManager) {
        instance = this;
        this.plugin = plugin;
        this.runesManager = runesManager;            // ← now assigning the real one
        this.effectRegistry = EffectRegistry.getInstance();
        loadSpells();
    }

    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }

    public Map<String, Spell> getSpellsByClass(String className) {
        return spellsByClass.getOrDefault(className.toLowerCase(), Collections.emptyMap());
    }

    public RunesManager getRunesManager() {
        return runesManager;
    }

    private void loadSpells() {

        // — COOLARCHER TEST CLASS —
        Map<String, Spell> coolMap = new HashMap<>();
        coolMap.put("BASIC_ATTACK", new Spell(
            "quick_shot",
            "Quick Shot",
            "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Quick_Shot"),
            1,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_QUICK_SHOT",
            0.0
        ));
        coolMap.put("LRL", new Spell(
            "backstep", "Backstep", "LRL",
            5.0,
            MythicSkillConfig.getCooldownSeconds("Backstep"), 3,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_BACKSTEP", 0.0
        ));
        coolMap.put("LRR", new Spell(
            "windrazor", "Windrazor", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Windrazor"), 5,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_WINDRAZOR", 0.0
        ));
        coolMap.put("LLR", new Spell(
            "arrow_barrage", "Arrow Barrage", "LLR",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Arrow_Barrage"), 10,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_ARROW_BARRAGE", 0.0
        ));
        coolMap.put("RRR", new Spell(
            "deadly_javelin", "Deadly Javelin", "RRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Deadly_Javelin"), 10,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_DEADLY_JAVELIN", 0.0
        ));
        coolMap.put("LLL", new Spell(
            "dragon_piercer", "Dragon Piercer", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Dragon_Piercer"), 10,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_DRAGON_PIERCER", 0.0
        ));
        spellsByClass.put("coolarcher", Collections.unmodifiableMap(coolMap));
        plugin.getLogger().info("[SPELLS] CoolArcher combos: " + coolMap.keySet());

        // — PHOENIXHUNTER CLASS —
        Map<String, Spell> phoenixMap = new HashMap<>();
        phoenixMap.put("BASIC_ATTACK", new Spell(
            "blazing_feathers",
            "Blazing Feathers",
            "BASIC_ATTACK",
            0.0,
            1,
            1,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_BLAZING_FEATHERS",
            0.0
        ));
        phoenixMap.put("LRL", new Spell(
            "ashdance", "Ashdance", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Ashdance"), 3,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_ASHDANCE", 0.0
        ));
        phoenixMap.put("LRR", new Spell(
            "flameburst_convergence", "Flameburst Convergence", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Flameburst_Convergence"), 5,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_FLAMEBURST_CONVERGENCE", 0.0
        ));
        phoenixMap.put("LLR", new Spell(
            "phoenix_totem", "Phoenix Totem", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Phoenix_Totem"), 0,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_PHOENIX_TOTEM", 0.0,
            true
        ));
        phoenixMap.put("LLL", new Spell(
            "pyroclasmic_barrage", "Pyroclasmic Barrage", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Pyroclasmic_Barrage"), 10,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_PYROCLASMIC_BARRAGE", 0.0
        ));
        phoenixMap.put("RRR", new Spell(
            "phoenix_rebirth", "Phoenix Rebirth", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Phoenix_Rebirth"), 10,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_PHOENIX_REBIRTH", 0.0
        ));
        spellsByClass.put("phoenixhunter", Collections.unmodifiableMap(phoenixMap));
        plugin.getLogger().info("[SPELLS] PhoenixHunter combos: " + phoenixMap.keySet());

        // — WARRIOR CLASS —
        Map<String, Spell> warriorMap = new HashMap<>();
        warriorMap.put("BASIC_ATTACK", new Spell(
            "brutal_strike", "Brutal Strike", "BASIC_ATTACK",
            0.0,
            1,
            1,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BRUTAL_STRIKE", 0.0
        ));
        warriorMap.put("LRL", new Spell(
            "charge", "Charge", "LRL",
            5.0,
            MythicSkillConfig.getCooldownSeconds("Charge"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_CHARGE", 0.0
        ));
        warriorMap.put("LRR", new Spell(
            "chain_hook", "Chain Hook", "LRR",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Chain_Hook"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_CHAIN_HOOK", 0.0
        ));
        warriorMap.put("LLR", new Spell(
            "shield_barrier", "Shield Barrier", "LLR",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Shield_Barrier"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_SHIELD_BARRIER", 0.0
        ));
        warriorMap.put("RLL", new Spell(
            "shockwave", "Shockwave", "RLL",
            14.0,
            MythicSkillConfig.getCooldownSeconds("Whirlwind"), 3,
            WeaponType.SWORD.getMaterials(),
            "SHOCKWAVE", 1.5
        ));
        warriorMap.put("LLL", new Spell(
            "judgement", "Judgement", "LLL",
            15.0,
            MythicSkillConfig.getCooldownSeconds("Judgement"), 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_JUDGEMENT", 0.0
        ));
        warriorMap.put("RRR", new Spell(
            "rampage", "Rampage", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Rampage"), 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_RAMPAGE", 0.0
        ));
        spellsByClass.put("warrior", Collections.unmodifiableMap(warriorMap));
        plugin.getLogger().info("[SPELLS] Warrior combos: " + warriorMap.keySet());

        // — BARBARIAN CLASS —
        Map<String, Spell> barbarianMap = new HashMap<>();
        barbarianMap.put("BASIC_ATTACK", new Spell(
            "rageblade", "Rageblade", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Rageblade"),
            1,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_RAGEBLADE", 0.0
        ));
        barbarianMap.put("LRL", new Spell(
            "primal_axe", "Primal Axe", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Primal_Axe"), 3,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_PRIMAL_AXE", 0.0
        ));
        barbarianMap.put("LLR", new Spell(
            "war_cry", "War Cry", "LLR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("War_Cry"), 5,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_WAR_CRY", 0.0
        ));
        barbarianMap.put("LLL", new Spell(
            "double_edge", "Double Edge", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Double_Edge"), 5,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_DOUBLE_EDGE", 0.0
        ));
        barbarianMap.put("RLL", new Spell(
            "relentless_leap", "Relentless Leap", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Relentless_Leap"), 3,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_RELENTLESS_LEAP", 0.0
        ));
        barbarianMap.put("RRR", new Spell(
            "eternal_fury", "Eternal Fury", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Eternal_Fury"), 10,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_ETERNAL_FURY", 0.0
        ));
        spellsByClass.put("barbarian", Collections.unmodifiableMap(barbarianMap));
        plugin.getLogger().info("[SPELLS] Barbarian combos: " + barbarianMap.keySet());

        // — PALADIN CLASS —
        Map<String, Spell> paladinMap = new HashMap<>();
        paladinMap.put("BASIC_ATTACK", new Spell(
            "holy_strike", "Holy Strike", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Holy_Strike"),
            1,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_HOLY_STRIKE", 0.0
        ));
        paladinMap.put("LRL", new Spell(
            "bound_seal", "Bound Seal", "LRL",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Bound_Seal"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BOUND_SEAL", 0.0
        ));
        paladinMap.put("LRR", new Spell(
            "hammer_of_justice", "Hammer Of Justice", "LRR",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Hammer_Of_Justice"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_HAMMER_OF_JUSTICE", 0.0
        ));
        paladinMap.put("LLL", new Spell(
            "heavenly_shield", "Heavenly Shield", "LLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Heavenly_Shield"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_HEAVENLY_SHIELD", 0.0
        ));
        paladinMap.put("RLL", new Spell(
            "unbreakable_will", "Unbreakable Will", "RLL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Unbreakable_Will"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_UNBREAKABLE_WILL", 0.0
        ));
        paladinMap.put("RRR", new Spell(
            "last_stand", "Last Stand", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Last_Stand"), 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_LAST_STAND", 0.0
        ));
        spellsByClass.put("paladin", Collections.unmodifiableMap(paladinMap));
        plugin.getLogger().info("[SPELLS] Paladin combos: " + paladinMap.keySet());

        // — DEATH KNIGHT CLASS —
        Map<String, Spell> dkMap = new HashMap<>();
        dkMap.put("BASIC_ATTACK", new Spell(
            "death_strike", "Death Strike", "BASIC_ATTACK",
            0.0,
            MythicSkillConfig.getCooldownSeconds("Death_Strike_ST"),
            1,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_DEATH_STRIKE", 0.0
        ));
        dkMap.put("LRL", new Spell(
            "phantom_charge", "Phantom Charge", "LRL",
            6.0,
            MythicSkillConfig.getCooldownSeconds("Phantom_Charge"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_PHANTOM_CHARGE", 0.0
        ));
        dkMap.put("LRR", new Spell(
            "wraithbound_chains", "Wraithbound Chains", "LRR",
            8.0,
            MythicSkillConfig.getCooldownSeconds("Wraithbound_Chains"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_WRAITHBOUND_CHAINS", 0.0
        ));
        dkMap.put("LLL", new Spell(
            "soul_barrier", "Soul Barrier", "LLL",
            10.0,
            MythicSkillConfig.getCooldownSeconds("Soul_Barrier"), 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_SOUL_BARRIER", 0.0
        ));
        dkMap.put("RLL", new Spell(
            "necrotic_whirlwind", "Necrotic Whirlwind", "RLL",
            12.0,
            MythicSkillConfig.getCooldownSeconds("Necrotic_Whirlwind"), 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_NECROTIC_WHIRLWIND", 0.0
        ));
        dkMap.put("RRR", new Spell(
            "death_sentence", "Death Sentence", "RRR",
            20.0,
            MythicSkillConfig.getCooldownSeconds("Death_Sentence"), 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_DEATH_SENTENCE", 0.0
        ));
        spellsByClass.put("deathknight", Collections.unmodifiableMap(dkMap));
        plugin.getLogger().info("[SPELLS] DeathKnight combos: " + dkMap.keySet());
    }

}
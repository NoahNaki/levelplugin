package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import me.nakilex.levelplugin.runes.model.RuneEffect;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
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
            1,
            1,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_QUICK_SHOT",
            0.0
        ));
        coolMap.put("LRL", new Spell(
            "backstep", "Backstep", "LRL",
            5.0,
            0, 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BACKSTEP", 0.0
        ));
        coolMap.put("LRR", new Spell(
            "windrazor", "Windrazor", "LRR",
            8.0,
            0, 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_WINDRAZOR", 0.0
        ));
        coolMap.put("LLR", new Spell(
            "arrow_barrage", "Arrow Barrage", "LLR",
            12.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_ARROW_BARRAGE", 0.0
        ));
        coolMap.put("RRR", new Spell(
            "deadly_javelin", "Deadly Javelin", "RRR",
            10.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_DEADLY_JAVELIN", 0.0
        ));
        coolMap.put("LLL", new Spell(
            "dragon_piercer", "Dragon Piercer", "LLL",
            15.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
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
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BLAZING_FEATHERS",
            0.0
        ));
        phoenixMap.put("LRL", new Spell(
            "ashdance", "Ashdance", "LRL",
            6.0,
            0, 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_ASHDANCE", 0.0
        ));
        phoenixMap.put("LRR", new Spell(
            "flameburst_convergence", "Flameburst Convergence", "LRR",
            8.0,
            0, 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_FLAMEBURST_CONVERGENCE", 0.0
        ));
        phoenixMap.put("LLR", new Spell(
            "phoenix_totem", "Phoenix Totem", "LLR",
            0.0,
            0, 0,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_PHOENIX_TOTEM", 0.0,
            true
        ));
        phoenixMap.put("LLL", new Spell(
            "pyroclasmic_barrage", "Pyroclasmic Barrage", "LLL",
            12.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_PYROCLASMIC_BARRAGE", 0.0
        ));
        phoenixMap.put("RRR", new Spell(
            "phoenix_rebirth", "Phoenix Rebirth", "RRR",
            20.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
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
            0, 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_CHARGE", 0.0
        ));
        warriorMap.put("LRR", new Spell(
            "chain_hook", "Chain Hook", "LRR",
            6.0,
            0, 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_CHAIN_HOOK", 0.0
        ));
        warriorMap.put("LLR", new Spell(
            "shield_barrier", "Shield Barrier", "LLR",
            0.0,
            0, 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_SHIELD_BARRIER", 0.0
        ));
        warriorMap.put("RLL", new Spell(
            "shockwave", "Shockwave", "RLL",
            14.0,
            0, 3,
            WeaponType.SWORD.getMaterials(),
            "SHOCKWAVE", 1.5
        ));
        warriorMap.put("LLL", new Spell(
            "judgement", "Judgement", "LLL",
            15.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_JUDGEMENT", 0.0
        ));
        warriorMap.put("RRR", new Spell(
            "rampage", "Rampage", "RRR",
            20.0,
            0, 10,
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
            1,
            1,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_RAGEBLADE", 0.0
        ));
        barbarianMap.put("LRL", new Spell(
            "primal_axe", "Primal Axe", "LRL",
            8.0,
            0, 3,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_PRIMAL_AXE", 0.0
        ));
        barbarianMap.put("LLR", new Spell(
            "war_cry", "War Cry", "LLR",
            10.0,
            0, 5,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_WAR_CRY", 0.0
        ));
        barbarianMap.put("LLL", new Spell(
            "double_edge", "Double Edge", "LLL",
            12.0,
            0, 5,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_DOUBLE_EDGE", 0.0
        ));
        barbarianMap.put("RLL", new Spell(
            "relentless_leap", "Relentless Leap", "RLL",
            6.0,
            0, 3,
            WeaponType.SHOVEL.getMaterials(),
            "MYTHIC_RELENTLESS_LEAP", 0.0
        ));
        barbarianMap.put("RRR", new Spell(
            "eternal_fury", "Eternal Fury", "RRR",
            20.0,
            0, 10,
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
            1,
            1,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_HOLY_STRIKE", 0.0
        ));
        paladinMap.put("LRL", new Spell(
            "bound_seal", "Bound Seal", "LRL",
            8.0,
            0, 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_BOUND_SEAL", 0.0
        ));
        paladinMap.put("LRR", new Spell(
            "hammer_of_justice", "Hammer Of Justice", "LRR",
            10.0,
            0, 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_HAMMER_OF_JUSTICE", 0.0
        ));
        paladinMap.put("LLL", new Spell(
            "heavenly_shield", "Heavenly Shield", "LLL",
            12.0,
            0, 5,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_HEAVENLY_SHIELD", 0.0
        ));
        paladinMap.put("RLL", new Spell(
            "unbreakable_will", "Unbreakable Will", "RLL",
            6.0,
            0, 3,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_UNBREAKABLE_WILL", 0.0
        ));
        paladinMap.put("RRR", new Spell(
            "last_stand", "Last Stand", "RRR",
            20.0,
            0, 10,
            WeaponType.SWORD.getMaterials(),
            "MYTHIC_LAST_STAND", 0.0
        ));
        spellsByClass.put("paladin", Collections.unmodifiableMap(paladinMap));
        plugin.getLogger().info("[SPELLS] Paladin combos: " + paladinMap.keySet());
    }

}
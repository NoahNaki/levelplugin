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

        // — WARRIOR SPELLS —
        Map<String, Spell> warriorMap = new HashMap<>();
        warriorMap.put("RLR", new Spell(
            "iron_fortress", "Iron Fortress", "RLR",
            15.0, 
            0, 10,
            me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
            "IRON_FORTRESS", 0.0
        ));
        warriorMap.put("RRR", new Spell(
            "heroic_leap", "Heroic Leap", "RRR",
            10.0, 
            0, 8,
            me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
            "HEROIC_LEAP", 1.2
        ));
        warriorMap.put("RRL", new Spell(
            "judgement", "Judgement", "RRL",
            16.0, 
            0, 15,
            me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
            "JUDGEMENT", 2.0
        ));
        warriorMap.put("RLL", new Spell(
            "shockwave", "Shockwave", "RLL",
            14.0,
            0, 3,
            me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
            "SHOCKWAVE", 1.5
        ));
        warriorMap.put("LRL", new Spell(
            "war_cry", "War Cry", "LRL",
            12.0,
            0, 5,
            me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
            "WAR_CRY", 0.0
        ));
        spellsByClass.put("warrior", Collections.unmodifiableMap(warriorMap));

        // — MAGE SPELLS (including BASIC_MAGE_ATTACK) —
        Map<String, Spell> mageMap = new HashMap<>();

        mageMap.put(
            "L",
            new Spell(
                "basic_ray",
                "Basic Ray",
                "L",
                0, 
                1,
                0,
                WeaponType.WAND.getMaterials(),
                "BASIC_RAY",
                1.0
            )
        );
        // Combo spells
        mageMap.put("RLL", new Spell(
            "meteor", "Meteor", "RLL",
            20.0, 
            0, 3,
            me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
            "METEOR", 5.5
        ));
        mageMap.put("RRL", new Spell(
            "blackhole", "Blackhole", "RRL",
            18.0, 
            0, 15,
            me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
            "BLACKHOLE", 3.0
        ));
        mageMap.put("RLR", new Spell(
            "heal", "Heal", "RLR",
            15.0, 
            0, 8,
            me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
            "HEAL", 0.0
        ));
        mageMap.put("RRR", new Spell(
            "teleport", "Teleport", "RRR",
            10.0, 
            0, 10,
            me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
            "TELEPORT", 0.0
        ));

        spellsByClass.put("mage", Collections.unmodifiableMap(mageMap));
        plugin.getLogger().info("[SPELLS] Mage combos: " + mageMap.keySet());


        // — ROGUE SPELLS —
        Map<String, Spell> rogueMap = new HashMap<>();
        rogueMap.put("RLL", new Spell(
            "phantom_blade", "Phantom Blade", "RLL",
            5.0,
            0, 1,
            me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
            "PHANTOM_BLADE", 2.0
        ));
        rogueMap.put("RRR", new Spell(
            "vanish", "Vanish", "RRR",
            8.0,
            0, 11,
            me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
            "VANISH", 0.0
        ));
        rogueMap.put("RRL", new Spell(
            "multihit", "Multihit", "RRL",
            8.0,
            0, 21,
            me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
            "MULTIHIT", 3.3
        ));
        rogueMap.put("RLR", new Spell(
            "smoke_bomb", "Smoke Bomb", "RLR",
            10.0,
            0, 31,
            me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
            "SMOKE_BOMB", 1.0
        ));
        spellsByClass.put("rogue", Collections.unmodifiableMap(rogueMap));

        // — ARCHER SPELLS —
        Map<String, Spell> archerMap = new HashMap<>();

        archerMap.put("BASIC_ATTACK", new Spell(
            "basic_arrow",
            "Basic Shot",
            "BASIC_ATTACK",
            0.0,
            1,
            1,
            WeaponType.BOW.getMaterials(),
            "BASIC_ATTACK",
            1.0
        ));
        archerMap.put("LLR", new Spell(
            "power_shot", "Power Shot", "LLR",
            12.0, 
            0, 3,
            me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
            "POWER_SHOT", 2.0
        ));
        archerMap.put("LRR", new Spell(
            "bow_drone", "Sentry", "LRR",
            15.0, 
            0, 8,
            me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
            "BOW_DRONE", 1.5
        ));
        archerMap.put("LLL", new Spell(
            "grapple_hook", "Grapple Hook", "LLL",
            8.0, 
            0, 10,
            me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
            "GRAPPLE_HOOK", 0.0
        ));
        archerMap.put("LRL", new Spell(
            "arrow_storm", "Arrow Storm", "LRL",
            20.0, 
            0, 15,
            me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
            "ARROW_STORM", 0.5
        ));
        spellsByClass.put("archer", Collections.unmodifiableMap(archerMap));
    }

}
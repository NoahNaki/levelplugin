package me.nakilex.levelplugin.spells.managers;

import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.spells.Spell;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SpellManager {

    private static SpellManager instance;
    public static SpellManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SpellManager has not been initialized!");
        }
        return instance;
    }

    // Map: className -> (combo -> Spell)
    private final Map<String, Map<String, Spell>> spellsByClass = new HashMap<>();

    public SpellManager(Plugin plugin) {
        instance = this;
        loadSpells();
    }

    private void loadSpells() {
        // Global mana multiplier (e.g. +20% per consecutive cast)
        final double defaultManaMultiplier = 1.2;

        // Warrior Spells (shovels)
        spellsByClass.put("warrior", Map.of(
            "RLR", new Spell(
                "iron_fortress", "Iron Fortress", "RLR",
                15.0, defaultManaMultiplier,
                0, 10,
                WeaponType.SHOVEL.getMaterials(),    // <-- now from WeaponType
                "IRON_FORTRESS", 0.0
            ),
            "RRR", new Spell(
                "heroic_leap", "Heroic Leap", "RRR",
                10.0, defaultManaMultiplier,
                0, 8,
                WeaponType.SHOVEL.getMaterials(),
                "HEROIC_LEAP", 1.2
            ),
            "RRL", new Spell(
                "uppercut", "Uppercut", "RRL",
                15.0, defaultManaMultiplier,
                0, 15,
                WeaponType.SHOVEL.getMaterials(),
                "UPPERCUT", 1.3
            ),
            "RLL", new Spell(
                "ground_slam", "Ground Slam", "RLL",
                14.0, defaultManaMultiplier,
                0, 3,
                WeaponType.SHOVEL.getMaterials(),
                "GROUND_SLAM", 1.5
            )
        ));

        // Mage Spells (wands)
        spellsByClass.put("mage", Map.of(
            "RLL", new Spell(
                "meteor", "Meteor", "RLL",
                20.0, defaultManaMultiplier,
                0, 3,
                WeaponType.WAND.getMaterials(),
                "METEOR", 5.5
            ),
            "RRL", new Spell(
                "blackhole", "Blackhole", "RRL",
                18.0, defaultManaMultiplier,
                0, 15,
                WeaponType.WAND.getMaterials(),
                "BLACKHOLE", 0.0
            ),
            "RLR", new Spell(
                "heal", "Heal", "RLR",
                15.0, defaultManaMultiplier,
                0, 8,
                WeaponType.WAND.getMaterials(),
                "HEAL", 0.0
            ),
            "RRR", new Spell(
                "teleport", "Teleport", "RRR",
                10.0, defaultManaMultiplier,
                0, 10,
                WeaponType.WAND.getMaterials(),
                "TELEPORT", 0.0
            )
        ));

        // Rogue Spells (swords)
        spellsByClass.put("rogue", Map.of(
            "RRL", new Spell(
                "endless_assault", "Endless Assault", "RRL",
                12.0, defaultManaMultiplier,
                0, 15,
                WeaponType.SWORD.getMaterials(),
                "ENDLESS_ASSAULT", 3.3
            ),
            "RLL", new Spell(
                "blade_fury", "Blade Fury", "RLL",
                15.0, defaultManaMultiplier,
                0, 3,
                WeaponType.SWORD.getMaterials(),
                "BLADE_FURY", 2.5
            ),
            "RLR", new Spell(
                "shadow_clone", "Shadow Clone", "RLR",
                10.0, defaultManaMultiplier,
                0, 10,
                WeaponType.SWORD.getMaterials(),
                "SHADOW_CLONE", 0.0
            ),
            "RRR", new Spell(
                "vanish", "Vanish", "RRR",
                8.0, defaultManaMultiplier,
                0, 8,
                WeaponType.SWORD.getMaterials(),
                "VANISH", 0.0
            )
        ));

        // Archer Spells (bows)
        spellsByClass.put("archer", Map.of(
            "LLR", new Spell(
                "power_shot", "Power Shot", "LLR",
                12.0, defaultManaMultiplier,
                0, 3,
                WeaponType.BOW.getMaterials(),
                "POWER_SHOT", 2.0
            ),
            "LRR", new Spell(
                "bow_drone", "Sentry", "LRR",
                15.0, defaultManaMultiplier,
                0, 8,
                WeaponType.BOW.getMaterials(),
                "BOW_DRONE", 1.5
            ),
            "LLL", new Spell(
                "grapple_hook", "Grapple Hook", "LLL",
                8.0, defaultManaMultiplier,
                0, 10,
                WeaponType.BOW.getMaterials(),
                "GRAPPLE_HOOK", 0.0
            ),
            "LRL", new Spell(
                "arrow_storm", "Arrow Storm", "LRL",
                20.0, defaultManaMultiplier,
                0, 15,
                WeaponType.BOW.getMaterials(),
                "ARROW_STORM", 0.5
            )
        ));
    }


    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }

    public Map<String, Spell> getSpellsByClass(String className) {
        return spellsByClass.getOrDefault(className.toLowerCase(), Collections.emptyMap());
    }
}

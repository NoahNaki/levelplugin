package me.nakilex.levelplugin.spells.managers;

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

        // Warrior Spells
        spellsByClass.put("warrior", Map.of(
            "RLR", new Spell(
                "iron_fortress", "Iron Fortress", "RLR",
                15.0, defaultManaMultiplier,  // baseCost, multiplier
                0, 10,                        // cooldown, levelReq
                Arrays.asList(
                    Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
                ),
                "IRON_FORTRESS", 0.0         // effectKey, damageMultiplier
            ),
            "RRR", new Spell(
                "heroic_leap", "Heroic Leap", "RRR",
                10.0, defaultManaMultiplier,
                0, 8,
                Arrays.asList(
                    Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
                ),
                "HEROIC_LEAP", 1.2
            ),
            "RRL", new Spell(
                "uppercut", "Uppercut", "RRL",
                15.0, defaultManaMultiplier,
                0, 15,
                Arrays.asList(
                    Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
                ),
                "UPPERCUT", 1.3
            ),
            "RLL", new Spell(
                "ground_slam", "Ground Slam", "RLL",
                14.0, defaultManaMultiplier,
                0, 3,
                Arrays.asList(
                    Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                    Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
                ),
                "GROUND_SLAM", 1.5
            )
        ));

        // Mage Spells
        spellsByClass.put("mage", Map.of(
            "RLL", new Spell(
                "meteor", "Meteor", "RLL",
                20.0, defaultManaMultiplier,
                0, 3,
                Arrays.asList(Material.STICK, Material.BLAZE_ROD),
                "METEOR", 5.5
            ),
            "RRL", new Spell(
                "blackhole", "Blackhole", "RRL",
                18.0, defaultManaMultiplier,
                0, 15,
                Arrays.asList(Material.STICK, Material.BLAZE_ROD),
                "BLACKHOLE", 0.0
            ),
            "RLR", new Spell(
                "heal", "Heal", "RLR",
                15.0, defaultManaMultiplier,
                0, 8,
                Arrays.asList(Material.STICK, Material.BLAZE_ROD),
                "HEAL", 0.0
            ),
            "RRR", new Spell(
                "teleport", "Teleport", "RRR",
                10.0, defaultManaMultiplier,
                0, 10,
                Arrays.asList(Material.STICK, Material.BLAZE_ROD),
                "TELEPORT", 0.0
            )
        ));

        // Rogue Spells
        spellsByClass.put("rogue", Map.of(
            "RRL", new Spell(
                "endless_assault", "Endless Assault", "RRL",
                12.0, defaultManaMultiplier,
                0, 15,
                Arrays.asList(
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
                ),
                "ENDLESS_ASSAULT", 3.3
            ),
            "RLL", new Spell(
                "blade_fury", "Blade Fury", "RLL",
                15.0, defaultManaMultiplier,
                0, 3,
                Arrays.asList(
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
                ),
                "BLADE_FURY", 2.5
            ),
            "RLR", new Spell(
                "shadow_clone", "Shadow Clone", "RLR",
                10.0, defaultManaMultiplier,
                0, 10,
                Arrays.asList(
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
                ),
                "SHADOW_CLONE", 0.0
            ),
            "RRR", new Spell(
                "vanish", "Vanish", "RRR",
                8.0, defaultManaMultiplier,
                0, 8,
                Arrays.asList(
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
                ),
                "VANISH", 0.0
            )
        ));

        // Archer Spells
        spellsByClass.put("archer", Map.of(
            "LLR", new Spell(
                "power_shot", "Power Shot", "LLR",
                12.0, defaultManaMultiplier,
                0, 3,
                Arrays.asList(Material.BOW, Material.CROSSBOW),
                "POWER_SHOT", 2.0
            ),
            "LRR", new Spell(
                "explosive_arrow", "Explosive Arrow", "LRR",
                15.0, defaultManaMultiplier,
                0, 8,
                Arrays.asList(Material.BOW, Material.CROSSBOW),
                "EXPLOSIVE_ARROW", 1.5
            ),
            "LLL", new Spell(
                "grapple_hook", "Grapple Hook", "LLL",
                8.0, defaultManaMultiplier,
                0, 10,
                Arrays.asList(Material.BOW, Material.CROSSBOW),
                "GRAPPLE_HOOK", 0.0
            ),
            "LRL", new Spell(
                "arrow_storm", "Arrow Storm", "LRL",
                20.0, defaultManaMultiplier,
                0, 15,
                Arrays.asList(Material.BOW, Material.CROSSBOW),
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

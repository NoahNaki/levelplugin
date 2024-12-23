package me.nakilex.levelplugin.managers;

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

    // Map: class -> (combo -> Spell)
    private final Map<String, Map<String, Spell>> spellsByClass = new HashMap<>();

    public SpellManager(Plugin plugin) {
        instance = this;
        loadSpells(); // Load spells directly without YAML
    }

    private void loadSpells() {
        // Warrior Spells
        spellsByClass.put("warrior", Map.of(
            "RRR", new Spell("ground_slam", "Ground Slam", "RRR", 10, 5, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "GROUND_SLAM", 1.5),
            "RLR", new Spell("charge", "Charge", "RLR", 8, 4, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "CHARGE", 1.2),
            "RRL", new Spell("shield_wall", "Shield Wall", "RRL", 12, 10, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "SHIELD_WALL", 0.0),
            "RLL", new Spell("battle_cry", "Battle Cry", "RLL", 10, 6, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "BATTLE_CRY", 0.0)
        ));

        // Mage Spells
        spellsByClass.put("mage", Map.of(
            "RLL", new Spell("meteor", "Meteor", "RLL", 20, 0, 1, Arrays.asList(Material.STICK, Material.BLAZE_ROD), "METEOR", 2.5),
            "RRL", new Spell("blackhole", "Blackhole", "RRL", 18, 0, 1, Arrays.asList(Material.STICK, Material.BLAZE_ROD), "BLACKHOLE", 0.0),
            "RLR", new Spell("heal", "Heal", "RLR", 15, 0, 1, Arrays.asList(Material.STICK, Material.BLAZE_ROD), "HEAL", 0.0),
            "RRR", new Spell("teleport", "Teleport", "RRR", 10, 0, 1, Arrays.asList(Material.STICK, Material.BLAZE_ROD), "TELEPORT", 0.0)
        ));

        // Rogue Spells
        spellsByClass.put("rogue", Map.of(
            "RLL", new Spell("shadow_step", "Shadow Step", "RLL", 12, 6, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "SHADOW_STEP", 1.3),
            "RRL", new Spell("blade_fury", "Blade Fury", "RRL", 15, 8, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "BLADE_FURY", 1.5),
            "RLR", new Spell("smoke_bomb", "Smoke Bomb", "RLR", 10, 8, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "SMOKE_BOMB", 0.0),
            "RRR", new Spell("vanish", "Vanish", "RRR", 8, 6, 1, Arrays.asList(Material.IRON_SWORD, Material.DIAMOND_SWORD), "VANISH", 0.0)
        ));

        // Archer Spells
        spellsByClass.put("archer", Map.of(
            "LLR", new Spell("power_shot", "Power Shot", "LLR", 12, 5, 1, Arrays.asList(Material.BOW, Material.CROSSBOW), "POWER_SHOT", 2.0),
            "LRR", new Spell("explosive_arrow", "Explosive Arrow", "LRR", 15, 7, 1, Arrays.asList(Material.BOW, Material.CROSSBOW), "EXPLOSIVE_ARROW", 1.5),
            "LLL", new Spell("grapple_hook", "Grapple Hook", "LLL", 8, 5, 1, Arrays.asList(Material.BOW, Material.CROSSBOW), "GRAPPLE_HOOK", 0.0),
            "LRL", new Spell("arrow_storm", "Arrow Storm", "LRL", 20, 10, 1, Arrays.asList(Material.BOW, Material.CROSSBOW), "ARROW_STORM", 0.5)
        ));
    }

    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }
}

package me.nakilex.levelplugin.spells.managers;

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

    public static SpellManager getInstance() {
        if (instance == null) throw new IllegalStateException("SpellManager not init’d!");
        return instance;
    }

    public SpellManager(Plugin plugin, RunesManager runesManager) {
        instance         = this;
        this.runesManager = runesManager;            // ← now assigning the real one
        this.effectRegistry = EffectRegistry.getInstance();
        loadSpells();
    }

    // ← NEW helper: pull the Spell object by class/combo
    private Spell lookupSpell(Player p, String spellKey) {
        String cls = p.getMetadata("playerClass") /* or however you know “mage”, “rogue”… */
            .get(0).asString().toLowerCase();
        return spellsByClass.getOrDefault(cls, Map.of()).get(spellKey);
    }

    // ← NEW helper: base damage before runes
    public double computeBaseDamage(Player p, String spellKey) {
        Spell s = lookupSpell(p, spellKey);
        return (s == null ? 0 : s.getBaseDamage());
    }

    // ← NEW helper: base cooldown before runes
    public long computeBaseCooldown(Player p, String spellKey) {
        Spell s = lookupSpell(p, spellKey);
        return (s == null ? 0L : s.getCooldownSeconds());
    }

    // File: src/main/java/me/nakilex/levelplugin/spells/managers/SpellManager.java

    public void castSpell(Player player, String spellKey) {
        // 1) lookup the Spell object
        Spell spell = getSpell(
            StatsManager.getInstance()
                .getPlayerStats(player.getUniqueId())
                .playerClass.name().toLowerCase(),
            spellKey
        );
        if (spell == null) return;

        // 2) let the Spell handle everything (runes, mana, cooldown, effect)
        spell.castEffect(player);
    }


    public Spell getSpell(String className, String combo) {
        Map<String, Spell> classMap = spellsByClass.get(className.toLowerCase());
        if (classMap == null) return null;
        return classMap.get(combo);
    }

    public Map<String, Spell> getSpellsByClass(String className) {
        return spellsByClass.getOrDefault(className.toLowerCase(), Collections.emptyMap());
    }

    public RunesManager getRunesManager() { return runesManager; }

    private void loadSpells() {
        final double defaultManaMultiplier = 1.2;

        // Warrior Spells
        spellsByClass.put("warrior", Map.of(
            "RLR", new Spell(
                "iron_fortress", "Iron Fortress", "RLR",
                15.0, defaultManaMultiplier,
                0, 10,
                me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
                "IRON_FORTRESS", 0.0
            ),
            "RRR", new Spell(
                "heroic_leap", "Heroic Leap", "RRR",
                10.0, defaultManaMultiplier,
                0, 8,
                me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
                "HEROIC_LEAP", 1.2
            ),
            "RRL", new Spell(
                "uppercut", "Uppercut", "RRL",
                15.0, defaultManaMultiplier,
                0, 15,
                me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
                "UPPERCUT", 1.3
            ),
            "RLL", new Spell(
                "ground_slam", "Ground Slam", "RLL",
                14.0, defaultManaMultiplier,
                0, 3,
                me.nakilex.levelplugin.items.data.WeaponType.SHOVEL.getMaterials(),
                "GROUND_SLAM", 1.5
            )
        ));

        // Mage Spells
        spellsByClass.put("mage", Map.of(
            "RLL", new Spell(
                "meteor", "Meteor", "RLL",
                20.0, defaultManaMultiplier,
                0, 3,
                me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
                "METEOR", 5.5
            ),
            "RRL", new Spell(
                "blackhole", "Blackhole", "RRL",
                18.0, defaultManaMultiplier,
                0, 15,
                me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
                "BLACKHOLE", 0.0
            ),
            "RLR", new Spell(
                "heal", "Heal", "RLR",
                15.0, defaultManaMultiplier,
                0, 8,
                me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
                "HEAL", 0.0
            ),
            "RRR", new Spell(
                "teleport", "Teleport", "RRR",
                10.0, defaultManaMultiplier,
                0, 10,
                me.nakilex.levelplugin.items.data.WeaponType.WAND.getMaterials(),
                "TELEPORT", 0.0
            )
        ));

        // Rogue Spells
        spellsByClass.put("rogue", Map.of(
            "RRL", new Spell(
                "endless_assault", "Endless Assault", "RRL",
                12.0, defaultManaMultiplier,
                0, 15,
                me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
                "ENDLESS_ASSAULT", 3.3
            ),
            "RLL", new Spell(
                "blade_fury", "Blade Fury", "RLL",
                15.0, defaultManaMultiplier,
                0, 3,
                me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
                "BLADE_FURY", 2.5
            ),
            "RLR", new Spell(
                "shadow_clone", "Shadow Clone", "RLR",
                10.0, defaultManaMultiplier,
                0, 10,
                me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
                "SHADOW_CLONE", 0.0
            ),
            "RRR", new Spell(
                "vanish", "Vanish", "RRR",
                8.0, defaultManaMultiplier,
                0, 8,
                me.nakilex.levelplugin.items.data.WeaponType.SWORD.getMaterials(),
                "VANISH", 0.0
            )
        ));

        // Archer Spells
        spellsByClass.put("archer", Map.of(
            "LLR", new Spell(
                "power_shot", "Power Shot", "LLR",
                12.0, defaultManaMultiplier,
                0, 3,
                me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
                "POWER_SHOT", 2.0
            ),
            "LRR", new Spell(
                "bow_drone", "Sentry", "LRR",
                15.0, defaultManaMultiplier,
                0, 8,
                me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
                "BOW_DRONE", 1.5
            ),
            "LLL", new Spell(
                "grapple_hook", "Grapple Hook", "LLL",
                8.0, defaultManaMultiplier,
                0, 10,
                me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
                "GRAPPLE_HOOK", 0.0
            ),
            "LRL", new Spell(
                "arrow_storm", "Arrow Storm", "LRL",
                20.0, defaultManaMultiplier,
                0, 15,
                me.nakilex.levelplugin.items.data.WeaponType.BOW.getMaterials(),
                "ARROW_STORM", 0.5
            )
        ));
    }
}
package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class Spell {

    private final String id;             // unique spell identifier
    private final String displayName;    // e.g. "Teleport"
    private final String combo;          // e.g. "RRR"

    // dynamic mana-cost fields
    private final double baseManaCost;
    private final double manaCostMultiplier;

    private final int cooldown;          // in seconds
    private final int levelReq;
    private final List<Material> allowedWeapons;
    private final String effectKey;
    private final double damageMultiplier;  // e.g. 1.5 = 150%

    public Spell(
        String id,
        String displayName,
        String combo,
        double baseManaCost,
        double manaCostMultiplier,
        int cooldown,
        int levelReq,
        List<Material> allowedWeapons,
        String effectKey,
        double damageMultiplier
    ) {
        this.id = id;
        this.displayName = displayName;
        this.combo = combo;
        this.baseManaCost = baseManaCost;
        this.manaCostMultiplier = manaCostMultiplier;
        this.cooldown = cooldown;
        this.levelReq = levelReq;
        this.allowedWeapons = allowedWeapons;
        this.effectKey = effectKey;
        this.damageMultiplier = damageMultiplier;
    }

    // Retrieve the current dynamic cost for this player and spell
    public double getCurrentManaCost(Player player) {
        return Main.getInstance()
            .getManaTracker()
            .getCost(player.getUniqueId(), id, baseManaCost);
    }

    // Expose base mana cost for UI and static references
    public double getBaseManaCost() {
        return baseManaCost;
    }

    // Alias for compatibility with SpellGUI
    public double getManaCost() {
        return baseManaCost;
    }

    // After a successful cast, bump the cost and schedule reset
    public void recordSpellCast(Player player) {
        Main.getInstance()
            .getManaTracker()
            .recordCast(player.getUniqueId(), id, baseManaCost);
    }

    // Attempt to cast: handle mana deduction and then apply effect
    public void castEffect(Player player) {
        double cost = getCurrentManaCost(player);
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());

        // Check mana availability
        if (ps.getCurrentMana() < cost) {
            player.sendMessage("§cNot enough mana to cast " + displayName + " (needs " + cost + ")");
            return;
        }

        // Deduct mana (round up to nearest int)
        int newMana = ps.getCurrentMana() - (int) Math.ceil(cost);
        ps.setCurrentMana(newMana);
        recordSpellCast(player);

        // Invoke the actual spell effect
        switch (effectKey.toUpperCase()) {
            case "GROUND_SLAM":
            case "HEROIC_LEAP":
            case "UPPERCUT":
            case "IRON_FORTRESS": {
                new WarriorSpell().castWarriorSpell(player, effectKey);
                break;
            }
            case "METEOR":
            case "BLACKHOLE":
            case "HEAL":
            case "TELEPORT":
            case "MAGE_BASIC": {
                new MageSpell().castMageSpell(player, effectKey);
                break;
            }
            case "ARROW_STORM":
            case "POWER_SHOT":
            case "GRAPPLE_HOOK":
            case "BOW_DRONE": {
                new ArcherSpell().castArcherSpell(player, effectKey);
                break;
            }
            case "VANISH":
            case "BLADE_FURY":
            case "ENDLESS_ASSAULT":
            case "SHADOW_CLONE": {
                new RogueSpell().castRogueSpell(player, effectKey);
                break;
            }
            default: {
                player.sendMessage("§eYou cast " + displayName + " (no effectKey logic)!");
            }
        }
    }

    // Standard getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getCombo() { return combo; }
    public int getCooldown() { return cooldown; }
    public int getLevelReq() { return levelReq; }
    public List<Material> getAllowedWeapons() { return allowedWeapons; }
    public double getDamageMultiplier() { return damageMultiplier; }
}

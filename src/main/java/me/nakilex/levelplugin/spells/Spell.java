package me.nakilex.levelplugin.spells;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Spell {

    private final String id;             // e.g. "ground_slam"
    private final String displayName;    // e.g. "Ground Slam"
    private final String combo;          // e.g. "RLR"
    private final int manaCost;
    private final int cooldown;          // in seconds
    private final int levelReq;
    private final List<Material> allowedWeapons; // which weapon materials are valid (e.g. all SHOVELS for warrior)
    private final Map<UUID, Long> mageBasicCooldown = new HashMap<>();


    private final String effectKey;
    private final double damageMultiplier;  // % weapon damage, e.g. 1.5 = 150%

    public Spell(
        String id,
        String displayName,
        String combo,
        int manaCost,
        int cooldown,
        int levelReq,
        List<Material> allowedWeapons,
        String effectKey,
        double damageMultiplier
    ) {
        this.id = id;
        this.displayName = displayName;
        this.combo = combo;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.levelReq = levelReq;
        this.allowedWeapons = allowedWeapons;
        this.effectKey = effectKey;
        this.damageMultiplier = damageMultiplier;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getLevelReq() {
        return levelReq;
    }

    public List<Material> getAllowedWeapons() {
        return allowedWeapons;
    }

    public void castEffect(Player player) {
        switch (effectKey.toUpperCase()) {
            case "GROUND_SLAM":
            case "HEROIC_LEAP":
            case "UPPERCUT":
            case "IRON_FORTRESS": {
                WarriorSpell warriorSpell = new WarriorSpell();
                warriorSpell.castWarriorSpell(player, effectKey);
                break;
            }
            case "METEOR":
            case "BLACKHOLE":
            case "HEAL":
            case "TELEPORT":
            case "MAGE_BASIC": {
                MageSpell mageSpell = new MageSpell();
                mageSpell.castMageSpell(player, effectKey);
                break;
            }
            case "ARROW_STORM":
            case "POWER_SHOT":
            case "GRAPPLE_HOOK":
            case "EXPLOSIVE_ARROW": {
                ArcherSpell archerSpell = new ArcherSpell();
                archerSpell.castArcherSpell(player, effectKey);
                break;
            }
            case "VANISH":
            case "BLADE_FURY":
            case "SHADOW_STEP":
            case "DAGGER_THROW": {
                RogueSpell rogueSpell = new RogueSpell();
                rogueSpell.castRogueSpell(player, effectKey);
                break;
            }
            default: {
                player.sendMessage("§eYou cast " + displayName + " (no effectKey logic)!");
            }
        }
    }


}

package me.nakilex.levelplugin.player.classes.data;

import me.nakilex.levelplugin.items.data.WeaponType;
import org.bukkit.inventory.ItemStack;

/** Utility methods related to player classes and families. */
public final class ClassUtil {
    private ClassUtil() {}

    /** Returns true if the given class is part of the Warrior family. */
    public static boolean isWarriorFamily(PlayerClass cls) {
        return cls == PlayerClass.WARRIOR
                || cls == PlayerClass.BARBARIAN
                || cls == PlayerClass.DRAGONIAN
                || cls == PlayerClass.GALEGLAIVE
                || cls == PlayerClass.DEATHKNIGHT
                || cls == PlayerClass.ARCTICKNIGHT
                || cls == PlayerClass.DRAGONWARRIOR
                || cls == PlayerClass.AWAKWARRIOR;
    }

    /** Returns true if the class belongs to the Archer family. */
    public static boolean isArcherFamily(PlayerClass cls) {
        return cls == PlayerClass.ARCHER
                || cls == PlayerClass.DEADEYE
                || cls == PlayerClass.PHOENIXHUNTER
                || cls == PlayerClass.AWAKARCHER;
    }

    /** Returns true if the class belongs to the Rogue family. */
    public static boolean isRogueFamily(PlayerClass cls) {
        return cls == PlayerClass.ROGUE
                || cls == PlayerClass.ABYSSION
                || cls == PlayerClass.AWAKROGUE;
    }

    /** Returns true if the class belongs to the Mage family. */
    public static boolean isMageFamily(PlayerClass cls) {
        return cls == PlayerClass.MAGE
                || cls == PlayerClass.WITCH
                || cls == PlayerClass.AWAKMAGE
                || cls == PlayerClass.ARCHMAGE;
    }

    /**
     * Returns a flavor text describing the default combat action for the given class.
     * Warrior and rogue families swing blades, archers loose arrows and mages cast spells.
     * All other classes default to swinging a blade.
     */
    public static String getAttackPhrase(PlayerClass cls) {
        if (isMageFamily(cls)) return "casting that spell";
        if (isArcherFamily(cls)) return "loosing that arrow";
        // Warriors, rogues and any other classes default to blade-based combat.
        return "swinging that blade";
    }

    /**
     * Determine if a player's class meets the required class, accounting for
     * subclasses. A null or VILLAGER requirement means all classes are allowed.
     */
    public static boolean meetsRequirement(PlayerClass playerClass, PlayerClass required) {
        if (required == null || required == PlayerClass.VILLAGER) return true;
        if (playerClass == required) return true;
        return switch (required) {
            case WARRIOR -> isWarriorFamily(playerClass);
            case ARCHER -> isArcherFamily(playerClass);
            case ROGUE -> isRogueFamily(playerClass);
            case MAGE -> isMageFamily(playerClass);
            case CLERIC -> playerClass == PlayerClass.CLERIC;
            default -> false;
        };
    }

    /**
     * Determine whether the supplied weapon type matches the expected family for a class.
     */
    public static boolean isValidWeaponForClass(PlayerClass playerClass, ItemStack weapon) {
        WeaponType type = WeaponType.matchType(weapon);
        if (type == null || playerClass == null) {
            return false;
        }
        if (isMageFamily(playerClass)) {
            return type == WeaponType.WAND;
        }
        if (isArcherFamily(playerClass)) {
            return type == WeaponType.BOW;
        }
        if (isRogueFamily(playerClass)) {
            return type == WeaponType.SWORD;
        }
        if (isWarriorFamily(playerClass)) {
            return type == WeaponType.SWORD
                    || type == WeaponType.AXE
                    || type == WeaponType.SHOVEL;
        }
        return false;
    }

    /**
     * Determine whether a weapon can be used by a class when both family weapon type and
     * explicit item class requirement must pass.
     */
    public static boolean canUseWeapon(PlayerClass playerClass, ItemStack weapon, PlayerClass requiredClass) {
        return isValidWeaponForClass(playerClass, weapon) && meetsRequirement(playerClass, requiredClass);
    }
}

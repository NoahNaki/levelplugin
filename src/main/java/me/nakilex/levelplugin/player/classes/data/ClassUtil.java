package me.nakilex.levelplugin.player.classes.data;

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
                || cls == PlayerClass.DRAGONWARRIOR;
    }

    /** Returns true if the class belongs to the Archer family. */
    public static boolean isArcherFamily(PlayerClass cls) {
        return cls == PlayerClass.ARCHER
                || cls == PlayerClass.DEADEYE
                || cls == PlayerClass.PHOENIXHUNTER;
    }

    /** Returns true if the class belongs to the Rogue family. */
    public static boolean isRogueFamily(PlayerClass cls) {
        return cls == PlayerClass.ROGUE;
    }

    /** Returns true if the class belongs to the Mage family. */
    public static boolean isMageFamily(PlayerClass cls) {
        return cls == PlayerClass.MAGE
                || cls == PlayerClass.OVERLORD;
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
}

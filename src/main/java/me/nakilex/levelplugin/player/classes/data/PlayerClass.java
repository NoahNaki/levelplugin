package me.nakilex.levelplugin.player.classes.data;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public enum PlayerClass {
    VILLAGER(1),
    WARRIOR(1),
    ROGUE(1),
    MAGE(1),
    CLERIC(1),
    WITCH(1),
    GAMBLER(1),

    // awakened / advanced classes
    BARBARIAN(1),
    DRAGONIAN(1),
    GALEGLAIVE(1),
    DEATHKNIGHT(1),
    ARCTICKNIGHT(1),
    DRAGONWARRIOR(1),
    AWAKROGUE(1),
    AWAKWARRIOR(1),
    AWAKARCHER(1),
    AWAKMAGE(1),
    AWAKCLERIC(1),
    ARCHMAGE(1),
    DEADEYE(1),

    // legacy / misc classes kept for compatibility
    ARCHER(1),
    PHOENIXHUNTER(1),
    PALADIN(1),
    ABYSSION(1);

    private final int requiredLevel;

    PlayerClass(int reqLvl) {
        this.requiredLevel = reqLvl;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    /** Human-friendly display name for the class. */
    public String getDisplayName() {
        String name = name();
        if (name.startsWith("AWAK")) {
            return "Awakened " + me.nakilex.levelplugin.utils.TextUtil.beautifyWords(name.substring(4));
        }
        return me.nakilex.levelplugin.utils.TextUtil.beautifyWords(name);
    }

    public boolean isAwakened() {
        return name().startsWith("AWAK");
    }

    public static PlayerClass randomAwakened(Random random) {
        List<PlayerClass> awakened = Arrays.stream(values())
                .filter(PlayerClass::isAwakened)
                .toList();
        if (awakened.isEmpty()) {
            return WARRIOR;
        }
        Random rng = random == null ? new Random() : random;
        return awakened.get(rng.nextInt(awakened.size()));
    }

    /**
     * Parse a class name case-insensitively.
     * Returns {@code null} if the name does not match any enum constant.
     */
    public static PlayerClass fromString(String name) {
        if (name == null) return null;
        try {
            return PlayerClass.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

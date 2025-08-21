package me.nakilex.levelplugin.player.classes.data;

public enum PlayerClass {
    VILLAGER(1),
    WARRIOR(1),
    ROGUE(1),
    MAGE(1),
    CLERIC(1),
    WITCH(1),

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

    /**
     * Parse a class name, providing backwards compatibility for legacy names.
     * Falls back to {@link #VILLAGER} if the name is unrecognized or null.
     */
    public static PlayerClass fromString(String name) {
        if (name == null) return null;
        String key = name.toUpperCase();
        return switch (key) {
            case "ASSASSIN" -> PlayerClass.ROGUE;
            case "AWAKASSASSIN" -> PlayerClass.AWAKROGUE;
            default -> {
                try {
                    yield PlayerClass.valueOf(key);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}

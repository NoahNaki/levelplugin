package me.nakilex.levelplugin.player.classes.data;

public enum PlayerClass {
    VILLAGER(1),
    WARRIOR(1),
    ROGUE(1),
    ARCHER(1),
    COOLARCHER(1),
    PHOENIXHUNTER(1),
    BARBARIAN(1),
    PALADIN(1),
    MAGE(1);

    private final int requiredLevel;

    PlayerClass(int reqLvl) {
        this.requiredLevel = reqLvl;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }
}

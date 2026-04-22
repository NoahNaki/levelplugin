package me.nakilex.levelplugin.stronghold;

public enum StrongholdQueueMode {
    SOLO("Solo", 1),
    DUO("Duo", 2),
    SQUAD("Squad", 4);

    private final String displayName;
    private final int teamSize;

    StrongholdQueueMode(String displayName, int teamSize) {
        this.displayName = displayName;
        this.teamSize = teamSize;
    }

    public String displayName() {
        return displayName;
    }

    public int teamSize() {
        return teamSize;
    }
}

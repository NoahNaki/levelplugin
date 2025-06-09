package me.nakilex.levelplugin.leaderboard;

public enum LeaderboardType {
    LEVEL("Levels"),
    DUELS("Duels Won"),
    BALANCE("Balance");

    private final String display;

    LeaderboardType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}

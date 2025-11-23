package me.nakilex.levelplugin.guild;

public enum GuildRole {
    LEADER,
    ADVISOR,
    VETERAN,
    MEMBER;

    public static GuildRole fromString(String name) {
        for (GuildRole r : values()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    public int getPriority() {
        return switch (this) {
            case LEADER -> 3;
            case ADVISOR -> 2;
            case VETERAN -> 1;
            case MEMBER -> 0;
        };
    }
}

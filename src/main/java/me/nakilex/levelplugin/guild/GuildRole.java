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
}

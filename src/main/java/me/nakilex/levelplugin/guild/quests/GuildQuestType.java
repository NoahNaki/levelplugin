package me.nakilex.levelplugin.guild.quests;

/** Types of objectives a guild quest can require. */
public enum GuildQuestType {
    LOOTCHEST_OPEN("Open Loot Chests", "pack1_scroll2"),
    KILL_MOBS("Slay Mobs", "pack1_scroll2"),
    COLLECT_RESOURCES("Collect Resources", "pack1_scroll2"),
    PARTICIPATE_SIEGE("Participate in a Siege", "pack1_scroll2"),
    WIN_DUELS("Win Duels", "pack1_scroll2");

    private final String displayName;
    private final String iconId;

    GuildQuestType(String displayName, String iconId) {
        this.displayName = displayName;
        this.iconId = iconId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconId() {
        return iconId;
    }
}

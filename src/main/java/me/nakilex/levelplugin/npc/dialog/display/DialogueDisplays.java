package me.nakilex.levelplugin.npc.dialog.display;

import org.bukkit.entity.Player;

/** Shared dialogue display access point used by messengers and the normal action-bar status task. */
public final class DialogueDisplays {
    private static final DialogueChatHistory HISTORY = new DialogueChatHistory();
    private static final DialogueDisplay DEFAULT = new ActionBarDialogueDisplay(HISTORY);

    private DialogueDisplays() { }

    public static DialogueDisplay defaultDisplay() { return DEFAULT; }
    public static DialogueChatHistory history() { return HISTORY; }
    public static boolean isActive(Player player) { return DEFAULT.isActive(player); }
}

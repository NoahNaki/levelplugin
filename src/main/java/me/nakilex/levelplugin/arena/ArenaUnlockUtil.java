package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Centralized arena unlock checks so both the command and GUI can enforce the
 * Yasiya quest prerequisite consistently.
 */
public final class ArenaUnlockUtil {

    private static final String YASIYA_QUEST_ID = "yasiyaarena";

    private ArenaUnlockUtil() {}

    public static boolean hasArenaAccess(Player player) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return true;
        }
        Quest quest = questManager.getQuest(YASIYA_QUEST_ID);
        if (quest == null) {
            return true;
        }
        QuestState state = questManager.getQuestState(player, quest);
        return state == QuestState.ACCEPTED
                || state == QuestState.IN_PROGRESS
                || state == QuestState.TURN_IN_READY
                || state == QuestState.COMPLETED;
    }

    public static boolean warnIfLocked(Player player) {
        if (hasArenaAccess(player)) {
            return false;
        }
        ChatMessageUtil.send(player, MessageType.WARNING,
                ChatColor.GRAY + "You haven't unlocked the arena yet. Complete Yasiya's quest first.");
        return true;
    }
}

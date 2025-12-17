package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;

/**
 * Base helper that stores shared dependencies for quest NPC handlers.
 */
public abstract class AbstractQuestNpcHandler implements QuestNpcHandler {
    private final String questId;
    protected final QuestManager questManager;
    protected final NPCDialogManager dialogManager;

    protected AbstractQuestNpcHandler(String questId, QuestManager questManager,
                                      NPCDialogManager dialogManager) {
        this.questId = questId;
        this.questManager = questManager;
        this.dialogManager = dialogManager;
    }

    @Override
    public String getQuestId() {
        return questId;
    }
}

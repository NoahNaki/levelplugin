package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that routes NPC interactions to quest-specific handlers.
 */
public class QuestNpcInteractionRegistry {
    private final Map<String, QuestNpcHandler> handlers = new HashMap<>();

    public QuestNpcInteractionRegistry register(QuestNpcHandler handler) {
        if (handler != null && handler.getQuestId() != null) {
            handlers.put(handler.getQuestId(), handler);
        }
        return this;
    }

    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (quest == null) {
            return false;
        }
        QuestNpcHandler handler = handlers.get(quest.getId());
        if (handler == null) {
            return false;
        }
        return handler.handle(player, npc, quest, state, questManager, dialogManager);
    }
}

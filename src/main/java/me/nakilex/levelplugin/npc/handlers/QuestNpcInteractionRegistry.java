package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
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

    public boolean handle(Player player, me.nakilex.levelplugin.npc.system.NPC npc,
                          net.citizensnpcs.api.npc.NPC citizensNpc,
                          Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (quest == null) {
            return false;
        }
        QuestNpcHandler handler = handlers.get(quest.getId());
        if (handler == null) {
            return false;
        }
        return handler.handle(player, npc, citizensNpc, quest, state, questManager, dialogManager);
    }
}

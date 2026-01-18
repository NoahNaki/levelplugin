package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

/**
 * Contract for quest-specific NPC interaction handlers.
 */
public interface QuestNpcHandler {
    /** Identifier of the quest this handler responds to. */
    String getQuestId();

    /**
     * Handle a player interacting with a quest-related NPC.
     *
     * @return true if the handler consumed the interaction.
     */
    boolean handle(Player player, me.nakilex.levelplugin.npc.system.NPC npc,
                   net.citizensnpcs.api.npc.NPC citizensNpc,
                   Quest quest, QuestState state,
                   QuestManager questManager, NPCDialogManager dialogManager);
}

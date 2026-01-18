package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

import java.util.List;

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

    protected int getNpcId(me.nakilex.levelplugin.npc.system.NPC npc,
                           net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            return npc.getId();
        }
        return citizensNpc != null ? citizensNpc.getId() : -1;
    }

    protected String getNpcName(me.nakilex.levelplugin.npc.system.NPC npc,
                                net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            return npc.getName();
        }
        return citizensNpc != null ? citizensNpc.getName() : null;
    }

    protected void startDialog(Player player, List<String> lines,
                               me.nakilex.levelplugin.npc.system.NPC npc,
                               net.citizensnpcs.api.npc.NPC citizensNpc,
                               Runnable finish) {
        if (npc != null) {
            dialogManager.startDialog(player, lines, npc, finish);
        } else if (citizensNpc != null) {
            dialogManager.startDialog(player, lines, citizensNpc, finish);
        }
    }

    protected void startQuestDialog(Player player, me.nakilex.levelplugin.quests.data.Quest quest,
                                    me.nakilex.levelplugin.npc.system.NPC npc,
                                    net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            dialogManager.startDialog(player, quest, npc);
        } else if (citizensNpc != null) {
            dialogManager.startDialog(player, quest, citizensNpc);
        }
    }

    protected void startChoiceDialog(Player player, List<String> options,
                                     me.nakilex.levelplugin.npc.system.NPC npc,
                                     net.citizensnpcs.api.npc.NPC citizensNpc,
                                     String questId, String flagBase,
                                     java.util.function.Consumer<Integer> callback) {
        if (npc != null) {
            dialogManager.startChoiceDialog(player, npc, options, questId, flagBase, callback);
        } else if (citizensNpc != null) {
            dialogManager.startChoiceDialog(player, citizensNpc, options, questId, flagBase, callback);
        }
    }

    protected boolean resumePendingChoice(Player player,
                                          me.nakilex.levelplugin.npc.system.NPC npc,
                                          net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            return dialogManager.resumePendingChoice(player, npc);
        }
        if (citizensNpc != null) {
            return dialogManager.resumePendingChoice(player, citizensNpc);
        }
        return false;
    }
}

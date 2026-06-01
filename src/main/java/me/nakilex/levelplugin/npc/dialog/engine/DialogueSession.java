package me.nakilex.levelplugin.npc.dialog.engine;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.quests.data.Quest;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class DialogueSession {
    public final UUID playerId;
    public final DialogueDefinition dialogue;
    public final NPC npc;
    public final net.citizensnpcs.api.npc.NPC citizensNpc;
    public final Quest quest;
    public final Runnable finish;
    public final Consumer<Integer> answerCallback;
    public final String choiceQuestId;
    public final String choiceFlagBase;
    public String currentPageId;
    public boolean typing;
    public int visibleCharacterCount;
    public int selectedAnswerIndex;
    public BukkitTask typingTask;
    public BukkitTask rangeTask;
    public boolean postActionsRun;
    public long openedAt;

    public DialogueSession(UUID playerId, DialogueDefinition dialogue, NPC npc,
                           net.citizensnpcs.api.npc.NPC citizensNpc, Quest quest, Runnable finish,
                           Consumer<Integer> answerCallback, String choiceQuestId, String choiceFlagBase) {
        this.playerId = playerId;
        this.dialogue = dialogue;
        this.npc = npc;
        this.citizensNpc = citizensNpc;
        this.quest = quest;
        this.finish = finish;
        this.answerCallback = answerCallback;
        this.choiceQuestId = choiceQuestId;
        this.choiceFlagBase = choiceFlagBase;
        this.currentPageId = dialogue.startPage();
        this.openedAt = System.currentTimeMillis();
    }

    public DialoguePage currentPage() {
        return dialogue.page(currentPageId);
    }

    public List<DialogueAnswer> visibleAnswers(DialogueConditionEvaluator evaluator) {
        return currentPage().answers().stream().filter(answer -> evaluator.matches(this, answer.conditions())).toList();
    }
}

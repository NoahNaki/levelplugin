package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.def.AbandonedCastleQuest;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Teaches players how to invest and upgrade essences.
 */
public class EssenceWeaversLessonQuest extends Quest implements QuestScript, QuestCompletionScript {
    public static final String ID = "essenceweaverslesson";
    public static final String NPC_NAME = "Essence Weaver";
    public static final int NPC_ID = 3769;

    private static final String INTRO_TARGET = "npc_essence_weaver_intro";
    private static final String RETURN_TARGET = "npc_essence_weaver_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.UPGRADE, "essence", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public EssenceWeaversLessonQuest() {
        super(
                ID,
                "Essence Weaver's Lesson",
                "Use the essence altar to invest duplicates or upgrade stars, then learn to swap with F.",
                createObjectives(),
                6,
                List.of(),
                null,
                QuestRewardCompat.create(230, 130, 0, List.of()),
                null,
                List.of(
                        "Essence Weaver|Power isn't just found, it's coaxed out. Bring me your spare essences.",
                        "<player>|What do I do with them?",
                        "Essence Weaver|Right-click the altar to open my loom, invest a duplicate, or attempt a star upgrade.",
                        "Essence Weaver|Press F to swap essences mid-fight—after you try, return and I'll share more."
                ),
                false
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, NPC_NAME);
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, NPC_NAME);
    }

    public static List<String> getReturnDialog() {
        return List.of(
                "Essence Weaver|Feel that pull? Essences grow eager when tended.",
                "<player>|It was riskier than I expected.",
                "Essence Weaver|Swap with F whenever you need a different edge and keep investing—the stars will align."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // Require the player to talk to the Essence Weaver to progress.
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }

        boolean hasCastle = questManager.hasCompleted(player.getUniqueId(), AbandonedCastleQuest.ID)
                || questManager.getProgress(player.getUniqueId(), AbandonedCastleQuest.ID) != null;
        if (!hasCastle) {
            questManager.startQuest(player, AbandonedCastleQuest.ID);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Cedric in the nearby town wants to talk—head over and hear about the abandoned castle.");
            questManager.setTrackedQuest(player, AbandonedCastleQuest.ID);
        }
    }
}

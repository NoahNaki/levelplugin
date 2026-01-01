package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.def.AbandonedCastleQuest;
import me.nakilex.levelplugin.quests.def.HawieHermitCrabQuest;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.UPGRADE, "essence", 1),
                new QuestObjective(QuestObjectiveType.ESSENCE_SWAP, "ANY", 1)
        );
    }

    public EssenceWeaversLessonQuest() {
        super(
                ID,
                "Essence Weaver's Lesson",
                "Use the essence altar to invest duplicates, then swap to another class with F.",
                createObjectives(),
                6,
                List.of(HawieHermitCrabQuest.ID),
                null,
                QuestRewardCompat.create(230, 130, 0, List.of()),
                null,
                List.of(
                        "Essence Weaver|Power isn't just found, it's coaxed out. Bring me your spare essences.",
                        "<player>|What do I do with them?",
                        "Essence Weaver|Right-click the altar to open my loom, invest a duplicate, or attempt a star upgrade.",
                        "Essence Weaver|Use " + ChatMessageUtil.format(ChatMessageUtil.MessageType.INFO, "/essence")
                                + " to equip another essence before you swap.",
                        "Essence Weaver|Press your Swap Offhand key (default: F) to swap essences mid-fight—feel the shift and the lesson is complete."
                ),
                false
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, NPC_NAME);
    }

    @Override
    public void onStart(Player player, Main plugin) {
        StatsManager statsManager = StatsManager.getInstance();
        if (!statsManager.hasSecondEssenceSlotUnlocked(player.getUniqueId())) {
            statsManager.unlockSecondEssenceSlot(player.getUniqueId());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "You unlocked your second Essence Slot!");
        }
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

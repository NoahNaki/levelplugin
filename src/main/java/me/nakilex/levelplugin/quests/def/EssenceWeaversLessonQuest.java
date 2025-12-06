package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Teaches players how to invest and upgrade essences.
 */
public class EssenceWeaversLessonQuest extends Quest implements QuestScript {
    public static final String ID = "essenceweaverslesson";
    public static final String NPC_NAME = "Essence Weaver";

    private static final String INTRO_TARGET = "npc_essence_weaver_intro";
    private static final String RETURN_TARGET = "npc_essence_weaver_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.UPGRADE, "essence", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public EssenceWeaversLessonQuest() {
        super(
                ID,
                "Essence Weaver's Lesson",
                "Invest duplicate essences or upgrade their stars with the Essence Weaver.",
                createObjectives(),
                6,
                List.of(),
                null,
                QuestRewardCompat.create(230, 130, 0, List.of()),
                null,
                List.of(
                        "Essence Weaver|Power isn't just found, it's coaxed out. Bring me your spare essences.",
                        "<player>|What do I do with them?",
                        "Essence Weaver|Invest a duplicate or attempt a star upgrade so you feel how essences respond.",
                        "Essence Weaver|After you try, return and I'll share how to weave their strength wisely."
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
                "Essence Weaver|Fortune favors patience. Keep investing and the stars will align."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

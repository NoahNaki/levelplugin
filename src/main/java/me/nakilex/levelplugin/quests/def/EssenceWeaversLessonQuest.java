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
 * Short quest that introduces the Essence Weaver upgrade flow.
 */
public class EssenceWeaversLessonQuest extends Quest implements QuestScript {
    public static final String ID = "essenceweaverlesson";

    public static final String NPC_NAME = "Essence Weaver";

    public static final int TALK_INTRO_INDEX = 0;
    public static final int UPGRADE_INDEX = 1;
    public static final int TALK_RETURN_INDEX = 2;

    public static final String INTRO_TARGET = "npc_essence_weaver_intro";
    public static final String RETURN_TARGET = "npc_essence_weaver_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.ESSENCE_UPGRADE, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public EssenceWeaversLessonQuest() {
        super(
                ID,
                "Essence Weaving",
                "Learn how to invest, reroll, and reseal class essences.",
                createObjectives(),
                5,
                List.of(),
                null,
                QuestRewardCompat.create(190, 95, 0, List.of()),
                null,
                List.of(
                        "Essence Weaver|You carry echoes of power you barely understand.",
                        "<player>|These crystals? I've just been slotting them in.",
                        "Essence Weaver|They can be refined. Bring an essence here, invest another into it, or try a star upgrade.",
                        "Essence Weaver|Even resealing is possible. Test the loom once, then return to me."
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
                "Essence Weaver|The loom answers your touch now.",
                "<player>|The power in these shards is clearer already.",
                "Essence Weaver|Keep refining. Each weave brings you closer to mastery."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

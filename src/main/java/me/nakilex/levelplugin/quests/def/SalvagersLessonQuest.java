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
 * Short quest that teaches players how to salvage unwanted gear.
 */
public class SalvagersLessonQuest extends Quest implements QuestScript {
    public static final String ID = "salvagerslesson";

    public static final String NPC_NAME = "Salvager";

    public static final int TALK_INTRO_INDEX = 0;
    public static final int SALVAGE_INDEX = 1;
    public static final int TALK_RETURN_INDEX = 2;
    public static final int SALVAGE_AMOUNT = 3;

    public static final String INTRO_TARGET = "npc_salvager_intro";
    public static final String RETURN_TARGET = "npc_salvager_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.SALVAGE, "ANY", SALVAGE_AMOUNT),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public SalvagersLessonQuest() {
        super(
                ID,
                "Scrap Lessons",
                "Learn how to break down extra gear and turn it into profit.",
                createObjectives(),
                3,
                List.of(),
                null,
                QuestRewardCompat.create(120, 40, 0, List.of()),
                null,
                List.of(
                        "Salvager|Nothing is truly worthless. Even rusted blades still have a second life in them.",
                        "<player>|You make coin from junk?",
                        "Salvager|From junk, from splinters, from whatever you bring me. Salvage three pieces yourself and I'll show you the trick.",
                        "Salvager|Break them down at the salvager's bench, then come back and tell me what you found."
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
                "Salvager|See? Even scraps sparkle once you melt them down.",
                "<player>|The coins weren't bad either.",
                "Salvager|Keep bringing me your leftovers. There's profit in every shard."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

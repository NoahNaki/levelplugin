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

    /** Placeholder NPC ID; replace with the actual salvager NPC when available. */
    public static final int NPC_ID = 9999;

    private static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    private static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.SALVAGE, "ANY", 3),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public SalvagersLessonQuest() {
        super(
                ID,
                "Scrap Lessons",
                "Learn how to break down extra gear and turn it into profit.",
                createObjectives(),
                1,
                List.of(),
                null,
                QuestRewardCompat.create(120, 40, 0, List.of()),
                NPC_ID,
                List.of(
                        "Salvager|Nothing is truly worthless. Even rusted blades still have a second life in them.",
                        "<player>|You make coin from junk?",
                        "Salvager|From junk, from splinters, from whatever you bring me. Salvage three pieces yourself and I'll show you the trick.",
                        "Salvager|Break them down at the salvager's bench, then come back and tell me what you found."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

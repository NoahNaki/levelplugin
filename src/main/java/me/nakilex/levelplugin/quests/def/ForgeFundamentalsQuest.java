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
 * Walks players through the Blacksmith GUI.
 */
public class ForgeFundamentalsQuest extends Quest implements QuestScript {
    public static final String ID = "forgefundamentals";
    public static final String NPC_NAME = "Blacksmith";
    public static final int NPC_ID = 1501;

    private static final String INTRO_TARGET = "npc_blacksmith_intro";
    private static final String RETURN_TARGET = "npc_blacksmith_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.BLACKSMITH_SERVICE, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public ForgeFundamentalsQuest() {
        super(
                ID,
                "Forge Fundamentals",
                "Learn how to repair, reroll, or upgrade gear at the blacksmith.",
                createObjectives(),
                4,
                List.of(),
                null,
                QuestRewardCompat.create(160, 80, 0, List.of()),
                null,
                List.of(
                        "Blacksmith|Steel sings for those who respect it. Step up and I'll show you the basics.",
                        "<player>|My gear seems fine, but sure.",
                        "Blacksmith|Run a repair, reroll, or upgrade through my forge so you know the drill.",
                        "Blacksmith|Once you've used a service, circle back and we'll talk shop."
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
                "Blacksmith|Not bad. Keep your tools tuned and they'll never betray you.",
                "<player>|Guess I should visit more often.",
                "Blacksmith|Do that and your blades will thank you."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // Require the player to talk to the Blacksmith to progress.
    }
}

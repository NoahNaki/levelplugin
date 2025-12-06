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
 * Encourages players to help upgrade the town.
 */
public class StonemasonJudeauQuest extends Quest implements QuestScript {
    public static final String ID = "stonemasonjudeau";
    public static final String NPC_NAME = "Stonemason Judeau";

    private static final String INTRO_TARGET = "npc_stonemason_intro";
    private static final String RETURN_TARGET = "npc_stonemason_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.TOWN_UPGRADE, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public StonemasonJudeauQuest() {
        super(
                ID,
                "Foundations First",
                "Pitch in toward a town upgrade for Stonemason Judeau.",
                createObjectives(),
                5,
                List.of(),
                null,
                QuestRewardCompat.create(200, 110, 0, List.of()),
                null,
                List.of(
                        "Stonemason Judeau|Walls stay strong when everyone lays a brick.",
                        "<player>|Need another pair of hands?",
                        "Stonemason Judeau|Contribute to any town upgrade and prove you're invested in our foundations.",
                        "Stonemason Judeau|Come back after you've lent a hand and we'll plan the next layer."
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
                "Stonemason Judeau|That's the spirit. Every upgrade keeps the town standing tall.",
                "<player>|Happy to help.",
                "Stonemason Judeau|We'll build faster with folks like you chipping in."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

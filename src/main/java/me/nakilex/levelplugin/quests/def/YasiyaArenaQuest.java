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
 * Yasiya encourages players to prove themselves by completing an arena bout.
 */
public class YasiyaArenaQuest extends Quest implements QuestScript {

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.ARENA_MATCH, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc1108", 1, BeaconTargets.npc(1108))
        );
    }

    public YasiyaArenaQuest() {
        super(
                "yasiyaarena",
                "Gladiator's Greeting",
                "Complete a full arena match for Yasiya.",
                createObjectives(),
                22,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(1500, 1000, 0, List.of()),
                1108,
                List.of(
                        "Yasiya|Steel is forged in the arena, not by swapping campfire stories.",
                        "Yasiya|Use /arena to queue for a bout, finish it no matter the outcome, and show me you won't run when the crowd roars.",
                        "Yasiya|Return once you're done so I know you're still breathing and can vouch for you with the quartermasters."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // nothing extra
    }
}

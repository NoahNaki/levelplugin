package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.*;

/** Simple quest that unlocks the Dragonian class. */
public class DragonianQuest extends Quest {
    private static java.util.List<QuestObjective> createObjectives() {
        return java.util.List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc533", 1)
        );
    }

    public DragonianQuest() {
        super(
                "dragonianquest",
                "Dragon's Challenge",
                "Speak with the dragon to earn a new power.",
                createObjectives(),
                1,
                java.util.List.of(),
                null,
                QuestRewardCompat.create(200, 100, 0, java.util.List.of(),
                        java.util.List.of(PlayerClass.DRAGONIAN)),
                533,
                java.util.List.of("Greetings, mortal.", "Return to me and claim your reward."),
                false
        );
    }
}

package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;

import java.util.List;

/**
 * Second part of the introduction questline.
 */
public class NewBeginningPart2Quest extends Quest {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1),
                new QuestObjective(QuestObjectiveType.BUY, "class_weapon", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc600", 1)
        );
    }

    public NewBeginningPart2Quest() {
        super(
                "newbeginning2",
                "A New Beginning II",
                "Choose a class and gear up for adventure.",
                createObjectives(),
                1,
                List.of("newbeginning1"),
                null,
                QuestRewardCompat.create(150, 30, 0, List.of()),
                600,
                List.of(
                        "Hey adventurer!",
                        "First let's get you some equipment.",
                        "If you sold the clothes you have on right now they'd fetch a pretty penny.",
                        "This fabric rivals what the nobles wear, but since you saved my life I'll help you out.",
                        "Here's some coins. Head over to the merchant and buy some armor and a weapon, but first we need to know what /class you are",
                        "Choose your class"
                )
        );
    }
}

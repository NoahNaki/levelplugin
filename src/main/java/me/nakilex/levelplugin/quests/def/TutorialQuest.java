package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.List;

public class TutorialQuest extends Quest {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("world");
        return Arrays.asList(
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1,
                        new Location(world, 0, 65, 0)),
                new QuestObjective(QuestObjectiveType.BUY, "class_weapon", 1,
                        new Location(world, 10, 65, 0)),
                new QuestObjective(QuestObjectiveType.CAST, "any_spell", 1,
                        new Location(world, 20, 65, 0))
        );
    }

    public TutorialQuest() {
        super(
                "tutorial",
                "First Steps",
                "Learn the basics of the game.",
                createObjectives(),
                1,
                List.of(),
                null,
                new QuestReward(0, 500, 0, List.of(), List.of()),
                273,
                List.of(
                        "Altan: Hello, accept my quest please?",
                        "Altan: If you do, I'll give you many rewards!",
                        "Altan: Such as 100 coins and 100 xp!"
                )
        );
    }
}

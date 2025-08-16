package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.List;

public class TutorialQuest extends Quest {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        return Arrays.asList(
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1,
                        BeaconTargets.staticLoc(new Location(world, 0, 65, 0))),
                new QuestObjective(QuestObjectiveType.BUY, "class_weapon", 1,
                        BeaconTargets.staticLoc(new Location(world, 753, 98, -176))),
                new QuestObjective(QuestObjectiveType.KILL, "ZOMBIE", 1,
                        BeaconTargets.staticLoc(new Location(world, 763, 100, 0))),
                new QuestObjective(QuestObjectiveType.TALK, "npc273", 1,
                        BeaconTargets.npc(273))
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
                QuestRewardCompat.create(150, 500, 10, List.of()),
                273,
                List.of(
                        "Hello there! Welcome to the realm.",
                        "Choose a class and buy your first weapon.",
                        "Slay a zombie nearby then speak with me again."
                ),
                false
        );
    }
}

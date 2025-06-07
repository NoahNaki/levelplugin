package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.List;

public class CanyonCleanupQuest extends Quest {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        return Arrays.asList(
                new QuestObjective(QuestObjectiveType.KILL, "Canyon_Miner", 3,
                        new Location(world, 400, 75, 300)),
                new QuestObjective(QuestObjectiveType.KILL, "Drunken_Miner", 2,
                        new Location(world, 405, 75, 305)),
                new QuestObjective(QuestObjectiveType.TALK, "npc276", 1,
                        new Location(world, 400, 75, 310))
        );
    }

    public CanyonCleanupQuest() {
        super(
                "canyon_cleanup",
                "Canyon Cleanup",
                "Deal with the unruly miners in the canyon.",
                createObjectives(),
                5,
                List.of("timber_trouble"),
                null,
                new QuestReward(500, 1500, 10, List.of(), List.of()),
                276,
                List.of(
                        "The mines are overrun with rowdy workers.",
                        "Take down some Canyon and Drunken Miners.",
                        "Return when the job is done."
                )
        );
    }
}

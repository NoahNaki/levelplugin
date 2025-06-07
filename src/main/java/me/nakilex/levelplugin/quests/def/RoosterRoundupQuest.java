package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.List;

public class RoosterRoundupQuest extends Quest {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        return Arrays.asList(
                new QuestObjective(QuestObjectiveType.KILL, "Wild_Rooster", 3,
                        new Location(world, 150, 70, 200)),
                new QuestObjective(QuestObjectiveType.TALK, "npc274", 1,
                        new Location(world, 150, 70, 210))
        );
    }

    public RoosterRoundupQuest() {
        super(
                "rooster_roundup",
                "Rooster Roundup",
                "Help the farmer by thinning out the wild roosters.",
                createObjectives(),
                1,
                List.of("tutorial"),
                null,
                new QuestReward(200, 750, 0, List.of(), List.of()),
                274,
                List.of(
                        "Greetings adventurer!",
                        "Those wild roosters keep stealing our grain.",
                        "Take a few out and report back to me."
                )
        );
    }
}

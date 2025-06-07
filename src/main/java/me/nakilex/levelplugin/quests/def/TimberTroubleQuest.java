package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.List;

public class TimberTroubleQuest extends Quest {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        return Arrays.asList(
                new QuestObjective(QuestObjectiveType.CRAFT, "WOODEN_AXE", 1,
                        new Location(world, 320, 75, 235)),
                new QuestObjective(QuestObjectiveType.KILL, "Timberbud", 5,
                        new Location(world, 330, 75, 250)),
                new QuestObjective(QuestObjectiveType.TALK, "npc275", 1,
                        new Location(world, 320, 75, 240))
        );
    }

    public TimberTroubleQuest() {
        super(
                "timber_trouble",
                "Timber Trouble",
                "Craft an axe and clear out the mischievous Timberbuds.",
                createObjectives(),
                2,
                List.of("rooster_roundup"),
                null,
                new QuestReward(300, 1000, 5, List.of(), List.of()),
                275,
                List.of(
                        "Hey there! We need sturdy wood for the town.",
                        "Craft yourself a wooden axe and deal with those Timberbuds.",
                        "Once you're done, come back for your payment."
                )
        );
    }
}

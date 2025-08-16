package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;

public class SerasQuest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, "SLIME_COMMON", 10,
                        BeaconTargets.staticLoc(new Location(world, 820, 65, -120))),
                new QuestObjective(QuestObjectiveType.TALK, "npc538", 1)
        );
    }

    private static final List<String> TURN_IN_DIALOG = List.of(
            "Seras|Alright, you have some skill considering you completed that way faster than I was expecting.",
            "Seras|Perhaps I underestimated you, nonetheless, I'll have more tasks for you to complete later but for now I'm busy investigating something I cannot disclose.",
            "Seras|Good luck adventurer, I'm sure I'll be seeing you again."
    );

    public static List<String> getTurnInDialog() {
        return TURN_IN_DIALOG;
    }

    public SerasQuest() {
        super(
                "serashelp",
                "Seras' Request",
                "Help Seras clear the forest slimes.",
                createObjectives(),
                1,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(200, 100, 0, List.of()),
                538,
                List.of(
                        "What are you looking at huh?",
                        "Piwan sent you did he, ugh, that rascal is always giving me more trouble.",
                        "<player>|He told me you could give me some tasks to help out around here.",
                        "Yeah that's true, there's never a still moment in this village that's for sure.",
                        "Follow this path, you'll come across some slimes that have been infesting the forest recently, kill 10 of those and bring back their cores and I'll give you a reward."
                ),
                false
        );
    }

    @Override
    public void onStart(org.bukkit.entity.Player player, me.nakilex.levelplugin.Main plugin) {
        // No special start logic
    }

}

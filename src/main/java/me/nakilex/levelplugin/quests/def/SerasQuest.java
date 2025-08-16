package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;
import java.util.Map;

public class SerasQuest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, "SLIME_COMMON", 10,
                        BeaconTargets.staticLoc(new Location(world, 820, 65, -120))),
                new QuestObjective(QuestObjectiveType.TALK, "npc823_first", 1),
                new QuestObjective(QuestObjectiveType.KILL, "SLIME_KING", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc823_second", 1)
        );
    }

    private static final Map<Integer, List<String>> STAGE_DIALOGS = Map.of(
            1, List.of(
                    "Seras|Alright, you have some skill considering you completed that way faster than I was expecting.",
                    "Seras|Perhaps I underestimated you, how about something a little more challenging then?",
                    "Seras|I'm sure you've noticed already but there is a massive slime that has gotten out of control, the folks around here call it the Slime King,",
                    "Seras|if you can defeat that then I'll truly be impressed."),
            3, List.of(
                    "Seras|Alright, you've certainly proven yourself adventurer,",
                    "Seras|I wonder why I've never heard of someone as strong as you before...",
                    "Seras|I'm sure you have your reasons, nonetheless I must take care of some other matters for now,",
                    "Seras|I'm sure our paths will cross again."));

    public static List<String> getDialogForObjective(int objectiveIndex) {
        return STAGE_DIALOGS.getOrDefault(objectiveIndex, List.of());
    }

    public SerasQuest() {
        super(
                "serashelp",
                "Seras' Request",
                "Help Seras clear the forest slimes and defeat their king.",
                createObjectives(),
                1,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(200, 100, 0, List.of()),
                823,
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

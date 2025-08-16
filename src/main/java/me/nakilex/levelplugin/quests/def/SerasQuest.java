package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import java.util.List;

public class SerasQuest extends Quest implements QuestScript, QuestCompletionScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, "SLIME", 10),
                new QuestObjective(QuestObjectiveType.TALK, "npc538", 1)
        );
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

    @Override
    public void onComplete(org.bukkit.entity.Player player, me.nakilex.levelplugin.Main plugin) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(538);
        if (npc != null) {
            plugin.getDialogManager().startDialog(player,
                    List.of(
                            "Seras|Alright, you have some skill considering you completed that way faster than I was expecting.",
                            "Seras|Perhaps I underestimated you, nonetheless, I'll have more tasks for you to complete later but for now I'm busy investigating something I cannot disclose.",
                            "Seras|Good luck adventurer, I'm sure I'll be seeing you again."
                    ),
                    npc,
                    null);
        }
    }
}

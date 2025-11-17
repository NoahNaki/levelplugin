package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Rahir needs the dunes cleared of aggressive scorpid packs harassing caravans.
 */
public class RahirScorpidQuest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, "vp1_scorpid", 10),
                new QuestObjective(QuestObjectiveType.TALK, "npc1109", 1, BeaconTargets.npc(1109))
        );
    }

    public RahirScorpidQuest() {
        super(
                "rahirscorpid",
                "Scorpid Sweep",
                "Cull the scorpid packs that stalk Rahir's trade route.",
                createObjectives(),
                1,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(3000, 2500, 0, List.of()),
                1109,
                List.of(
                        "Rahir|There's an open field just beyond town where the sand stays warm and the vp1_scorpid crawl out in droves.",
                        "Rahir|The caravans can't leave while they're nesting there, so the trade road is bleeding coin.",
                        "Rahir|Take that field back—kill ten of the scorpions skittering behind the walls and then return so I know the route is safe.",
                        "Rahir|Do that and I'll pay you properly for every wagon that makes it through without venom in the wheels."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // nothing extra
    }
}

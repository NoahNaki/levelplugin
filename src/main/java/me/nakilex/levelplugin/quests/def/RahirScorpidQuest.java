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
                        "Rahir|See those jagged tracks in the dunes? That's the trail of the vp1_scorpid packs shadowing my caravan.",
                        "Rahir|Every sting poisons a courier and every delay costs me coin.",
                        "Rahir|Hunt down ten of those scorpids so the sands go quiet again, then report back so I can reopen the route.",
                        "Rahir|I'll gladly share a cut of the profits with anyone brave enough to keep the venom at bay."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // nothing extra
    }
}

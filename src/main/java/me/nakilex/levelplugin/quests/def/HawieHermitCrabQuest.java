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
 * Hawie's follow-up request that has players thin out the hermit crabs gnawing
 * on his newly rebuilt docks.
 */
public class HawieHermitCrabQuest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, "vp1_hermit_crab", 10),
                new QuestObjective(QuestObjectiveType.TALK, "npc1089", 1, BeaconTargets.npc(1089))
        );
    }

    public HawieHermitCrabQuest() {
        super(
                "hawiehermitcrabs",
                "Clattering Cleanup",
                "Help Hawie stop the hermit crabs from tearing up his docks.",
                createObjectives(),
                1,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(300, 250, 0, List.of()),
                1089,
                List.of(
                        "Hawie|The surf has been spitting out these chittering hermit crabs all morning.",
                        "Hawie|They gnaw through the planks I just patched and scare off every sailor brave enough to dock here.",
                        "Hawie|Head down the shoreline and crush ten of those vp1_hermit_crabs before they chew through anything else.",
                        "Hawie|Come back alive with good news and I'll make sure you're paid for the trouble."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // no special start logic
    }
}

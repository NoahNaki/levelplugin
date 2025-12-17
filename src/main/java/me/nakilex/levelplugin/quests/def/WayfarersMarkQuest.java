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
 * Introduces players to unlocking and using waystones for travel.
 */
public class WayfarersMarkQuest extends Quest implements QuestScript {
    public static final String ID = "wayfarersmark";

    /** Placeholder NPC ID; replace with the real wayfinder NPC when available. */
    public static final int NPC_ID = 9910;

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.WAYSTONE_UNLOCK, "ANY", 1),
                new QuestObjective(QuestObjectiveType.WAYSTONE_USE, "ANY", 1)
        );
    }

    public WayfarersMarkQuest() {
        super(
                ID,
                "Wayfarer's Mark",
                "Unlock a waystone and learn to travel between anchors.",
                createObjectives(),
                3,
                List.of(),
                null,
                QuestRewardCompat.create(200, 90, 0, List.of()),
                null,
                List.of(
                        "Wayfinder|You look like someone who hates walking more than they need to.",
                        "<player>|I'm listening...",
                        "Wayfinder|Waystones remember you once you've touched them. Unlock one, then use it to blink back here.",
                        "Wayfinder|Do that and you'll never be lost in these hills again."
                ),
                true,
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // No additional start actions required.
    }
}

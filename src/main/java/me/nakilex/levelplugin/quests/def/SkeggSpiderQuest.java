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
 * Skegg directs adventurers to clear out a nearby cavern filled with ice spiders.
 */
public class SkeggSpiderQuest extends Quest implements QuestScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, "lp_spider_ice", 10),
                new QuestObjective(QuestObjectiveType.TALK, "npc1111", 1, BeaconTargets.npc(1111))
        );
    }

    public SkeggSpiderQuest() {
        super(
                "skeggspiders",
                "Frozen Threadwork",
                "Clear the ice spider den for Skegg.",
                createObjectives(),
                1,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(10000, 6000, 0, List.of()),
                1111,
                List.of(
                        "Skegg|Follow this path behind me and keep your eyes on the frost-covered stones.",
                        "Skegg|There's a cavern carved into the glacier where lp_spider_ice broods are weaving webs across the trade route.",
                        "Skegg|Cut down ten of those icy crawlers so my caravans stop disappearing, then come straight back so I know the tunnel is clear."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // no custom start logic
    }
}

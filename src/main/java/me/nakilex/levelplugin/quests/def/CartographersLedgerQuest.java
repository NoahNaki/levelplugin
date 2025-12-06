package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Sends players to chart a nearby landmark and report back.
 */
public class CartographersLedgerQuest extends Quest implements QuestScript {
    public static final String ID = "cartographersledger";
    public static final String NPC_NAME = "Cartographer";
    public static final int NPC_ID = 9926;

    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";
    public static final String DISCOVERY_TARGET = "ledger_ruin_tablet";

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("mmorpg");
        Location overlook = new Location(world, 835.5, 72, -210.5);
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.EXPLORE, "NORTHERN_OVERLOOK", 1,
                        false, BeaconTargets.staticLoc(overlook), "Scout the overlook marked on the map."),
                new QuestObjective(QuestObjectiveType.DISCOVER, DISCOVERY_TARGET, 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public CartographersLedgerQuest() {
        super(
                ID,
                "Cartographer's Ledger",
                "Survey a ruin, log your findings, and return to the cartographer.",
                createObjectives(),
                5,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(210, 105, 0, List.of()),
                NPC_ID,
                List.of(
                        "Elynn|Maps are stories etched into paper.",
                        "<player>|Need another chapter written?",
                        "Elynn|Indeed. Scout the overlook north of town and mark any peculiar stone tablets you find.",
                        "Elynn|Bring back what you learn so I can chart it before scavengers pick it clean."
                ),
                false,
                true
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Elynn");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Elynn");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

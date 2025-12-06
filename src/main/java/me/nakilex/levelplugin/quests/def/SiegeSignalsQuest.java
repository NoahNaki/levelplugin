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
 * Encourages players to participate in a siege event and secure loot.
 */
public class SiegeSignalsQuest extends Quest implements QuestScript {
    public static final String ID = "siegesignals";
    public static final String NPC_NAME = "Captain";
    public static final int NPC_ID = 9927;

    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.SIEGE_PARTICIPATE, "ANY", 1),
                new QuestObjective(QuestObjectiveType.LOOTCHEST_OPEN, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public SiegeSignalsQuest() {
        super(
                ID,
                "Siege Signals",
                "Join a siege defense and secure a loot cache for the captain.",
                createObjectives(),
                10,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(260, 140, 0, List.of()),
                NPC_ID,
                List.of(
                        "Mara|Bandits have tested our walls all week.",
                        "<player>|Need an extra blade on the ramparts?",
                        "Mara|Absolutely. Join the next siege, grab a loot cache from the aftermath,",
                        "Mara|and report back so I know who can hold a line."
                ),
                false,
                true
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Mara");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Mara");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

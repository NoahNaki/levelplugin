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
 * Quest that introduces players to the dungeon entrance toll.
 */
public class DungeonGuardQuest extends Quest implements QuestScript {

    public static final String QUEST_ID = "dungeonguard";
    public static final int NPC_ID = 1114;
    public static final int REQUIRED_LEVEL = 30;
    public static final int ENTRY_FEE = 500;
    public static final String GATE_ID = "dungeonentrance";

    private static final List<String> INTRO_DIALOG = List.of(
            "Dungeon Guard|Hello adventurer, you'd like to enter the dungeon?",
            "Dungeon Guard|The entry fee costs 500 coins."
    );

    private static final List<String> TOO_WEAK_DIALOG = List.of(
            "Dungeon Guard|I'm sorry but you aren't strong enough to enter this dungeon yet.",
            "Dungeon Guard|Come back when you are level 50."
    );

    private static final List<String> DECLINE_DIALOG = List.of(
            "Dungeon Guard|I cannot let you pass without paying the entrance fee."
    );

    private static final List<String> APPROVAL_DIALOG = List.of(
            "Dungeon Guard|You may enter."
    );

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_entry", 1,
                        BeaconTargets.npc(NPC_ID))
        );
    }

    public DungeonGuardQuest() {
        super(
                QUEST_ID,
                "Guardian of the Depths",
                "Earn the Dungeon Guard's approval to access the trials below.",
                createObjectives(),
                REQUIRED_LEVEL,
                List.of(),
                null,
                QuestRewardCompat.create(0, 0, 0, List.of(), List.of(), List.of("Entry to the Dungeon")),
                NPC_ID,
                INTRO_DIALOG,
                false
        );
    }

    public static List<String> getIntroDialog() {
        return INTRO_DIALOG;
    }

    public static List<String> getTooWeakDialog() {
        return TOO_WEAK_DIALOG;
    }

    public static List<String> getDeclineDialog() {
        return DECLINE_DIALOG;
    }

    public static List<String> getApprovalDialog() {
        return APPROVAL_DIALOG;
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // No additional scripted behaviour.
    }
}

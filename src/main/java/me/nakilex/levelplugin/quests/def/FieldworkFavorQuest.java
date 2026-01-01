package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.farming.data.FarmingCrop;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Lifeskilling quest that has players harvest wheat for the farmstead.
 */
public class FieldworkFavorQuest extends Quest implements QuestScript {
    public static final String ID = "fieldworkfavor";
    public static final String NPC_NAME = "Farmer";
    public static final int NPC_ID = 4154;
    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";
    private static final String WHEAT_TARGET = FarmingCrop.WHEAT.getQuestId();

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.GATHER_CROPS, WHEAT_TARGET, 20),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public FieldworkFavorQuest() {
        super(
                ID,
                "Fieldwork Favor",
                "Help the farmer harvest the wheat field behind them.",
                createObjectives(),
                17,
                List.of(ForgeFundamentalsQuest.ID),
                null,
                QuestRewardCompat.create(900, 300, 0, List.of()),
                NPC_ID,
                List.of(
                        "Farmer|Glad you stopped by—this field behind me needs a steady hand.",
                        "Farmer|Take this tier-one scythe and harvest twenty wheat from the rows out back.",
                        "Farmer|Once you've gathered enough, come back and I'll settle up."
                ),
                false,
                true,
                true
        );
    }

    public static void registerTalkTargets(QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, NPC_NAME);
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, NPC_NAME);
    }

    public static List<String> getReturnDialog() {
        return List.of(
                "Farmer|That's a fine haul. The field looks a lot healthier already.",
                "<player>|All twenty bundles are harvested.",
                "Farmer|Good work. If you bring me more wheat, I've got buyers lined up."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // No special start logic required.
    }
}

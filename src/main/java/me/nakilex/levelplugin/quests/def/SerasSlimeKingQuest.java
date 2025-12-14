package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Part two of Seras' storyline: slay the Slime King after helping the nearby townsfolk.
 */
public class SerasSlimeKingQuest extends Quest implements QuestScript, QuestCompletionScript {
    public static final String ID = "serashelp_part2";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc" + SerasQuest.NPC_ID + "_second", 1,
                        BeaconTargets.npc(SerasQuest.NPC_ID)),
                new QuestObjective(QuestObjectiveType.KILL, "SLIME_KING", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc" + SerasQuest.NPC_ID + "_third", 1,
                        BeaconTargets.npc(SerasQuest.NPC_ID))
        );
    }

    private static final Map<Integer, List<String>> STAGE_DIALOGS = Map.of(
            2, List.of(
                    "Seras|That's one less giant slime terrorizing the trails.",
                    "Seras|You've proven yourself more than capable—I'll pass your name along to the town leadership.",
                    "Seras|Keep sharpening your skills; there will always be more threats out there."));

    public static List<String> getDialogForObjective(int objectiveIndex) {
        return STAGE_DIALOGS.getOrDefault(objectiveIndex, List.of());
    }

    public SerasSlimeKingQuest() {
        super(
                ID,
                "Seras' Request (Part 2)",
                "Return to Seras and slay the Slime King.",
                createObjectives(),
                7,
                List.of(SerasQuest.ID, StableKeeperQuest.ID, HawieHermitCrabQuest.ID),
                null,
                QuestRewardCompat.create(250, 175, 0, List.of()),
                SerasQuest.NPC_ID,
                List.of(
                        "Seras|Welcome back. Those roosters should be quiet now—ready for the real challenge?",
                        "Seras|The Slime King is still oozing around the grove. Put it down and report back to me."),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // no special start logic
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Seras|Impressive work. If you need steadier coin, the Stable Keeper and Hawie can vouch for you now.");
    }
}

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

/**
 * Hawie's follow-up request that has players thin out the hermit crabs gnawing
 * on his newly rebuilt docks.
 */
public class HawieHermitCrabQuest extends Quest implements QuestScript, QuestCompletionScript {
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
                10,
                List.of("serashelp", SalvagersLessonQuest.ID),
                null,
                QuestRewardCompat.create(300, 250, 0, List.of()),
                1089,
                List.of(
                        "Hawie|This little pond used to be peaceful, but now it's crawling with hermit crabs squatting in every tidepool.",
                        "Hawie|They rip shingles off my dock to make new shells and pinch the deckhands any time they reach for a mooring rope.",
                        "Hawie|Head down the shoreline, smash ten of the vp1_hermit_crabs stirring up the muck, and I'll pay you better than those pests deserve.",
                        "Hawie|Come back alive with good news and maybe we can hear the waves again instead of all that clattering."
                ),
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
        if (!questManager.hasCompleted(player.getUniqueId(), StableKeeperQuest.ID)
                && questManager.getProgress(player.getUniqueId(), StableKeeperQuest.ID) == null) {
            questManager.startQuest(player, StableKeeperQuest.ID);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Hawie thanks you and points you toward the Stable Keeper (NPC 652) for a new mount.");
        }
    }
}

package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.def.HawieHermitCrabQuest;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quest that unlocks the horse reroll service. */
public class StableKeeperQuest extends Quest implements QuestScript, QuestCompletionScript {
    public static final String ID = "stablekeeper";
    public static final String NPC_TALK_TARGET = "npc652";
    public static final String NPC_RETURN_TARGET = "npc652_first";
    public static final String NPC_FINAL_TARGET = "npc652_second";
    public static final String HORSE_BUY_TARGET = "stablekeeper_horse";

    public static final int TALK_INTRO_INDEX = 0;
    public static final int KILL_ROOSTERS_INDEX = 1;
    public static final int TALK_REPORT_INDEX = 2;
    public static final int BUY_HORSE_INDEX = 3;
    public static final int TALK_FINAL_INDEX = 4;

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, NPC_TALK_TARGET, 1,
                        BeaconTargets.npc(652)),
                new QuestObjective(QuestObjectiveType.KILL, "wild_rooster", 5),
                new QuestObjective(QuestObjectiveType.TALK, NPC_RETURN_TARGET, 1,
                        BeaconTargets.npc(652)),
                new QuestObjective(QuestObjectiveType.BUY, HORSE_BUY_TARGET, 1),
                new QuestObjective(QuestObjectiveType.TALK, NPC_FINAL_TARGET, 1,
                        BeaconTargets.npc(652))
        );
    }

    private static final Map<Integer, List<String>> STAGE_DIALOGS = Map.of(
            TALK_REPORT_INDEX, List.of(
                    "Stable Keeper|Finally, some breathing room. The hens are already venturing back to the troughs.",
                    "Stable Keeper|You've earned a mount. Step into my tack room and use the reroll stable to pick a horse.",
                    "Stable Keeper|I'll waive the fee this once—call it gratitude. After you've chosen a horse, report back for your reward."),
            TALK_FINAL_INDEX, List.of(
                    "Stable Keeper|That horse suits you. It even stopped eyeing me like a snack.",
                    "<player>|Thanks again for trusting me with it.",
                    "Stable Keeper|Keep her brushed and she'll carry you anywhere. Come back any time you need another reroll, but the next one won't be free."));

    public static List<String> getDialogForObjective(int objectiveIndex) {
        return STAGE_DIALOGS.getOrDefault(objectiveIndex, List.of());
    }

    public StableKeeperQuest() {
        super(
                ID,
                "Feathered Famine",
                "Help the Stable Keeper reclaim his wheat and earn your first horse.",
                createObjectives(),
                5,
                List.of(SerasQuest.ID),
                null,
                QuestRewardCompat.create(500, 150, 0, List.of()),
                652,
                List.of(
                        "Stable Keeper|Look at this mess... those feral roosters have pecked every wheat stalk I had left.",
                        "<player>|Can't your horses eat something else?",
                        "Stable Keeper|Not if I want them fast. No grain means no feed, and no feed means no horses.",
                        "Stable Keeper|Hunt down the wild roosters hiding near the tree line and cull five of them for me.",
                        "Stable Keeper|Bring me back some peace and I'll let you pick out a horse for free."
                ),
                false
        );
    }

    public static boolean hasUnlockedHorseMenu(UUID playerId) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return false;
        }
        if (questManager.hasCompleted(playerId, ID)) {
            return true;
        }
        PlayerQuestProgress progress = questManager.getProgress(playerId, ID);
        return progress != null && progress.getProgress(TALK_REPORT_INDEX) >= 1;
    }

    public static boolean shouldReceiveFreeReroll(UUID playerId) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return false;
        }
        if (questManager.hasCompleted(playerId, ID)) {
            return false;
        }
        PlayerQuestProgress progress = questManager.getProgress(playerId, ID);
        if (progress == null) {
            return false;
        }
        return progress.getProgress(TALK_REPORT_INDEX) >= 1
                && progress.getProgress(BUY_HORSE_INDEX) < 1;
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // Keep the intro objective active so players are guided to speak with the Stable Keeper first.
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }

        if (!questManager.hasCompleted(player.getUniqueId(), HawieHermitCrabQuest.ID)
                && questManager.getProgress(player.getUniqueId(), HawieHermitCrabQuest.ID) == null) {
            questManager.startQuest(player, HawieHermitCrabQuest.ID);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Stable Keeper|Hawie down by the docks needs help with hermit crabs—give him a hand then report back to Seras.");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Stable Keeper|Let Seras know the roosters won't be bothering us anymore.");
        }
    }
}

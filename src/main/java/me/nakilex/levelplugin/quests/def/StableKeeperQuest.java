package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StableKeeperQuest extends Quest implements QuestScript {

    public static final String QUEST_ID = "stablekeeper";
    public static final int NPC_ID = 900;

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.KILL, "wild_rooster", 5),
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_feed", 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.BUY, "horse_purchase", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_final", 1, BeaconTargets.npc(NPC_ID))
        );
    }

    private static final Map<Integer, List<String>> STAGE_DIALOGS = Map.of(
            2, List.of(
                    "Stable Keeper|You actually wrangled those feral birds? Bless you.",
                    "Stable Keeper|Without wheat my horses start nipping at each other, it's a disaster.",
                    "Stable Keeper|Here, take a look at my stock. I'll let you pull a fresh mount for free this once.",
                    "Stable Keeper|When you're saddled up come back so I know the stables are in good hooves."),
            4, List.of(
                    "Stable Keeper|Look at you, that mare suits you already.",
                    "Stable Keeper|Keep an eye on her feed and she'll carry you farther than any carriage.",
                    "Stable Keeper|Stop by anytime you want to roll the dice on a new steed—I'll keep a stall open for you."));

    public static List<String> getDialogForObjective(int index) {
        return STAGE_DIALOGS.getOrDefault(index, List.of());
    }

    public StableKeeperQuest() {
        super(
                QUEST_ID,
                "Feathered Famine",
                "Cull the wild roosters, earn the Stable Keeper's trust, and claim your first steed.",
                createObjectives(),
                1,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(50, 25, 0, List.of()),
                NPC_ID,
                List.of(
                        "Stable Keeper|Whoa there, easy girl... sorry, the horses spook easy when there's no feed.",
                        "Stable Keeper|Those blasted roosters keep raiding my grain, I've got nothing left to calm the herd.",
                        "Stable Keeper|If you can thin five of the wild roosters outside town I'll owe you more than a handful of coins.",
                        "Stable Keeper|Do that and I'll even let you draw a fresh horse from my stables for free.",
                        "<player>|You've got a deal, the roads are safer with you stocked up anyway."),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // No additional start logic
    }

    public static boolean hasUnlockedHorseMenu(QuestManager questManager, UUID playerId) {
        if (questManager.hasCompleted(playerId, QUEST_ID)) {
            return true;
        }
        PlayerQuestProgress progress = questManager.getProgress(playerId, QUEST_ID);
        if (progress == null) {
            return false;
        }
        Quest quest = progress.getQuest();
        if (quest == null || quest.getObjectives().size() <= 2) {
            return false;
        }
        return progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();
    }

    public static boolean hasFreeHorsePurchase(QuestManager questManager, UUID playerId) {
        PlayerQuestProgress progress = questManager.getProgress(playerId, QUEST_ID);
        if (progress == null) {
            return false;
        }
        Quest quest = progress.getQuest();
        if (quest == null || quest.getObjectives().size() <= 3) {
            return false;
        }
        boolean unlocked = progress.getProgress(2) >= quest.getObjectives().get(2).getAmount();
        boolean purchased = progress.getProgress(3) >= quest.getObjectives().get(3).getAmount();
        return unlocked && !purchased;
    }
}

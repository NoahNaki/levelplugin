package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;
import java.util.Map;

public class SerasQuest extends Quest implements QuestScript, QuestCompletionScript {
    public static final String ID = "serashelp";
    public static final int NPC_ID = 823;

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("world");
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID, 1,
                        BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.KILL, "rpg_rat", 10,
                        BeaconTargets.staticLoc(new Location(world, 140, 69, -215))),
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_first", 1,
                        BeaconTargets.npc(NPC_ID))
        );
    }

    private static final Map<Integer, List<String>> STAGE_DIALOGS = Map.of(
            2, List.of(
                    "Seras|Not bad, those rats won't be regrouping anytime soon.",
                    "Seras|If you want to really help the town, head to the Stable Keeper and cull the wild roosters.",
                    "Seras|Once you've handled that, come back to me—I've got a much bigger slime problem in mind."));

    public static List<String> getDialogForObjective(int objectiveIndex) {
        return STAGE_DIALOGS.getOrDefault(objectiveIndex, List.of());
    }

    public SerasQuest() {
        super(
                ID,
                "Seras' Request (Part 1)",
                "Help Seras clear the plague rats around town.",
                createObjectives(),
                2,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(300, 100, 0, List.of()),
                NPC_ID,
                List.of(
                        "What are you looking at huh?",
                        "Piwan sent you did he, ugh, that rascal is always giving me more trouble.",
                        "<player>|He told me you could give me some tasks to help out around here.",
                        "Yeah that's true, there's never a still moment in this village that's for sure.",
                        "Follow this path, you'll come across some plague rats that have been infesting the forest recently, kill 10 of those and bring back their tails and I'll give you a reward."
                ),
                false,
                true,
                true
        );
    }

    @Override
    public void onStart(org.bukkit.entity.Player player, me.nakilex.levelplugin.Main plugin) {
        // No special start logic
    }

    @Override
    public void onComplete(org.bukkit.entity.Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        if (!questManager.hasCompleted(player.getUniqueId(), StableKeeperQuest.ID)
                && questManager.getProgress(player.getUniqueId(), StableKeeperQuest.ID) == null) {
            questManager.startQuest(player, StableKeeperQuest.ID);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Seras|Check in with the Stable Keeper before hunting down those roosters.");
        }
    }

}

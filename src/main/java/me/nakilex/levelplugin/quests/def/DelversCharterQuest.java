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
 * Points players toward creating and clearing a dungeon run.
 */
public class DelversCharterQuest extends Quest implements QuestScript {
    public static final String ID = "delverscharter";
    public static final String NPC_NAME = "Scout";

    /** Placeholder NPC ID; replace with the dungeon scout in-game. */
    public static final int NPC_ID = 9930;

    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.DUNGEON_CREATE, "ANY", 1),
                new QuestObjective(QuestObjectiveType.LOOTCHEST_OPEN, "dungeon", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public DelversCharterQuest() {
        super(
                ID,
                "Delver's Charter",
                "Form a dungeon party, claim a run, and crack open a loot chest inside.",
                createObjectives(),
                12,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(270, 150, 0, List.of()),
                NPC_ID,
                List.of(
                        "Scout Liora|Caves are swallowing caravans. We need delvers with initiative.",
                        "<player>|Point me to a hole in the ground.",
                        "Scout Liora|Use the dungeon board to spin up an instance, rally whoever answers, then pry open a loot chest inside.",
                        "Scout Liora|Bring me whatever scraps you find so I know the tunnels are thinned."
                ),
                false,
                true
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Scout Liora");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Scout Liora");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

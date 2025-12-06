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
 * Teaches players to repair and reroll gear with the blacksmith.
 */
public class ForgeFundamentalsQuest extends Quest implements QuestScript {
    public static final String ID = "forgefundamentals";
    public static final String NPC_NAME = "Forge Master";

    /** Placeholder NPC ID; replace with the blacksmith trainer in-game. */
    public static final int NPC_ID = 9925;

    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.BLACKSMITH_REPAIR, "ANY", 1),
                new QuestObjective(QuestObjectiveType.BLACKSMITH_REROLL, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public ForgeFundamentalsQuest() {
        super(
                ID,
                "Forge Fundamentals",
                "Learn to repair and reroll your equipment with the blacksmith.",
                createObjectives(),
                4,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(190, 95, 0, List.of()),
                NPC_ID,
                List.of(
                        "Bram|Steel keeps you alive longer than bravado.",
                        "<player>|My gear has seen better days.",
                        "Bram|Then let's fix that. Repair one item with my anvil, reroll another at the forge,",
                        "Bram|and you'll stop looking like a walking scrap heap."
                ),
                false,
                true
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Bram");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Bram");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

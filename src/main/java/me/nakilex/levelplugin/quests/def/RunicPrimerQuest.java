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
 * Teaches players how to equip and remove runes on their gear.
 */
public class RunicPrimerQuest extends Quest implements QuestScript {
    public static final String ID = "runicprimer";

    /** Placeholder NPC ID; replace with the actual rune mentor NPC when assigned. */
    public static final int NPC_ID = 9911;

    private static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    private static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.RUNE_EQUIP, "ANY", 1),
                new QuestObjective(QuestObjectiveType.RUNE_UNEQUIP, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public RunicPrimerQuest() {
        super(
                ID,
                "Runic Primer",
                "Practice equipping and removing runes to tune your gear.",
                createObjectives(),
                1,
                List.of(),
                null,
                QuestRewardCompat.create(180, 75, 0, List.of()),
                NPC_ID,
                List.of(
                        "Rune Scholar|Runes are fickle—they bond to steel only if you treat them right.",
                        "<player>|I could use a primer.",
                        "Rune Scholar|Socket any rune you have, then pull it free without cracking it.",
                        "Rune Scholar|Return once you've felt the rhythm of swapping them."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

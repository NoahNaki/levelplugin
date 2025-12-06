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
 * Onboards players to core spellcasting loops.
 */
public class RunicResonanceQuest extends Quest implements QuestScript {
    public static final String ID = "runicresonance";
    public static final String NPC_NAME = "Arcanist";

    /** Placeholder NPC ID; replace with the arcanist trainer in-game. */
    public static final int NPC_ID = 9928;

    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.CAST, "ANY", 1),
                new QuestObjective(QuestObjectiveType.CAST_COMBO, "ANY", 1),
                new QuestObjective(QuestObjectiveType.CONSUME_POTION, "mana", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public RunicResonanceQuest() {
        super(
                ID,
                "Runic Resonance",
                "Cast a spell, weave a combo, and learn to keep your mana topped off.",
                createObjectives(),
                7,
                List.of("newbeginning"),
                null,
                QuestRewardCompat.create(230, 120, 0, List.of()),
                NPC_ID,
                List.of(
                        "Thalion|Magic sings when you push it beyond a single spark.",
                        "<player>|Show me the chorus, then.",
                        "Thalion|Loose any spell, then chain a combo so your hands remember the rhythm.",
                        "Thalion|Drink a mana draught if you flag. Return once you feel the hum in your bones."
                ),
                false,
                true
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Thalion");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Thalion");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

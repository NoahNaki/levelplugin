package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;

import java.util.List;

public class StrongholdInitiationQuest extends Quest {
    public static final String ID = "strongholdinitiation";
    public static final int NPC_ID = 999001; // Placeholder NPC id. Replace in production.

    public StrongholdInitiationQuest() {
        super(
                ID,
                "Stronghold Initiation",
                "Prove yourself by surviving your first Stronghold push.",
                List.of(
                        new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID, 1, BeaconTargets.npc(NPC_ID)),
                        new QuestObjective(QuestObjectiveType.STRONGHOLD_ENTER, "ANY", 1),
                        new QuestObjective(QuestObjectiveType.STRONGHOLD_WAVE_CLEAR, "ANY", 5),
                        new QuestObjective(QuestObjectiveType.STRONGHOLD_KEY_USE, "ANY", 1),
                        new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_return", 1, BeaconTargets.npc(NPC_ID))
                ),
                8,
                List.of(),
                null,
                QuestRewardCompat.create(450, 250, 15, List.of()),
                null,
                List.of(
                        "Stronghold Warden|The gate listens only to proven hands.",
                        "<player>|How do I prove it?",
                        "Stronghold Warden|Step inside, clear a few waves, use a key on a sealed gate, then report back.",
                        "Stronghold Warden|Do that, and I'll recognize you as a real defender."
                ),
                true,
                false
        );
    }
}

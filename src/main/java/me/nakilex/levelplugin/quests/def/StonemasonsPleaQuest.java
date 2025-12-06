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
 * Guides players to invest in town upgrades.
 */
public class StonemasonsPleaQuest extends Quest implements QuestScript {
    public static final String ID = "stonemasonsplea";
    public static final String NPC_NAME = "Quartermaster";

    /** Placeholder NPC ID; replace with your town board/quartermaster NPC. */
    public static final int NPC_ID = 9929;

    public static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    public static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.TOWN_UPGRADE, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
        );
    }

    public StonemasonsPleaQuest() {
        super(
                ID,
                "Stonemason's Plea",
                "Contribute to a town upgrade to shore up the settlement's defenses.",
                createObjectives(),
                6,
                List.of("marketbeginnings"),
                null,
                QuestRewardCompat.create(240, 130, 0, List.of()),
                NPC_ID,
                List.of(
                        "Quartermaster Rynn|Wood rots, walls crumble. Gold keeps stone standing.",
                        "<player>|Tell me where to put my coin.",
                        "Quartermaster Rynn|Invest in any town upgrade—barricades, banners, I don't care—as long as mortar gets mixed.",
                        "Quartermaster Rynn|Bring me the receipt so I can mark you as someone who pulls their weight."
                ),
                false,
                true
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Quartermaster Rynn");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Quartermaster Rynn");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

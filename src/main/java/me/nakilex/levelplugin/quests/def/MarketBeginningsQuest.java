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
 * Walks players through listing and bidding in the auction house.
 */
public class MarketBeginningsQuest extends Quest implements QuestScript {
    public static final String ID = "marketbeginnings";

    /** Placeholder NPC ID; replace with the actual auctioneer NPC when placed. */
    public static final int NPC_ID = 1363;

    private static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    private static final String RETURN_TARGET = "npc" + NPC_ID + "_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.AUCTION_LIST, "ANY", 1),
                new QuestObjective(QuestObjectiveType.AUCTION_BID, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public MarketBeginningsQuest() {
        super(
                ID,
                "Market Beginnings",
                "Learn how to list items and bid for deals at the auction house.",
                createObjectives(),
                6,
                List.of(),
                null,
                QuestRewardCompat.create(210, 110, 0, List.of()),
                NPC_ID,
                List.of(
                        "Auctioneer|Every fortune starts with a first listing.",
                        "<player>|I don't want to get swindled.",
                        "Auctioneer|List any spare item to get a feel for our fees, then place a bid on something you like.",
                        "Auctioneer|Once you've danced with the market a bit, come back and I'll share a tip or two."
                ),
                false
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

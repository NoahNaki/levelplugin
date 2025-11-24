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
    public static final String NPC_NAME = "Auction House";

    public static final int TALK_INTRO_INDEX = 0;
    public static final int LIST_INDEX = 1;
    public static final int BID_INDEX = 2;
    public static final int TALK_RETURN_INDEX = 3;

    public static final String INTRO_TARGET = "npc_auction_house_intro";
    public static final String RETURN_TARGET = "npc_auction_house_return";

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_NAME)),
                new QuestObjective(QuestObjectiveType.AUCTION_LIST, "ANY", 1),
                new QuestObjective(QuestObjectiveType.AUCTION_BUY, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_NAME))
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
                null,
                List.of(
                        "Auctioneer|Every fortune starts with a first listing.",
                        "<player>|I don't want to get swindled.",
                        "Auctioneer|List any spare item to get a feel for our fees, then place a bid on something you like.",
                        "Auctioneer|Once you've danced with the market a bit, come back and I'll share a tip or two."
                ),
                false
        );
    }

    public static void registerTalkTargets(me.nakilex.levelplugin.quests.managers.QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, "Auctioneer");
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, "Auctioneer");
    }

    public static List<String> getReturnDialog() {
        return List.of(
                "Auctioneer|Not bad for a newcomer. Those coins will start flowing faster now.",
                "<player>|Any secrets to bidding?",
                "Auctioneer|Watch the timers, trust your gut, and never bid more than you'd celebrate losing."
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        plugin.getQuestManager().handleTalk(player, INTRO_TARGET);
    }
}

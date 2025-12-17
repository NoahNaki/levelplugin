package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.MarketBeginningsQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestServiceAccessTracker;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

/**
 * Handles the auction house tutorial quest flow.
 */
public class MarketBeginningsNpcHandler extends AbstractQuestNpcHandler {

    private final AuctionHouseGUI auctionGUI;

    public MarketBeginningsNpcHandler(QuestManager questManager, NPCDialogManager dialogManager,
                                      AuctionHouseGUI auctionGUI) {
        super(MarketBeginningsQuest.ID, questManager, dialogManager);
        this.auctionGUI = auctionGUI;
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        boolean completed = questManager.hasCompleted(player.getUniqueId(), MarketBeginningsQuest.ID);
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), MarketBeginningsQuest.ID);
        boolean introDone = progress != null && progress.getProgress(MarketBeginningsQuest.TALK_INTRO_INDEX) >= 1;
        boolean listed = progress != null && progress.getProgress(MarketBeginningsQuest.LIST_INDEX) >= 1;
        boolean bid = progress != null && progress.getProgress(MarketBeginningsQuest.BID_INDEX) >= 1;
        boolean returned = progress != null && progress.getProgress(MarketBeginningsQuest.TALK_RETURN_INDEX) >= 1;
        boolean cooling = QuestServiceAccessTracker.isCoolingDown(player.getUniqueId(), QuestServiceAccessTracker.Service.AUCTION);

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> questManager.handleTalk(player, MarketBeginningsQuest.INTRO_TARGET));
            return true;
        }

        if (completed || returned) {
            if (cooling) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Hold on, the auctioneer is sorting paperwork.");
                return true;
            }
            if (auctionGUI != null) {
                auctionGUI.open(player);
            }
            return true;
        }

        if (!listed || !bid) {
            if (!cooling) {
                if (auctionGUI != null) {
                    auctionGUI.open(player);
                }
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Give the auction house a moment before reopening.");
            }
            return true;
        }

        dialogManager.startDialog(player,
                MarketBeginningsQuest.getReturnDialog(),
                npc,
                () -> questManager.handleTalk(player, MarketBeginningsQuest.RETURN_TARGET));
        return true;
    }
}

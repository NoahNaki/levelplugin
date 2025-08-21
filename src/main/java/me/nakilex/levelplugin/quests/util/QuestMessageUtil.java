package me.nakilex.levelplugin.quests.util;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Utility for emitting quest completion messages with reward breakdowns.
 * Designed for reuse by normal quests and guild quests.
 */
public final class QuestMessageUtil {

    private QuestMessageUtil() {}

    /**
     * Send a formatted completion message listing guild and personal rewards.
     *
     * @param player     target player
     * @param header     centered title line (e.g. "§6§lQuest Complete!")
     * @param questName  human readable quest name
     * @param guildExp   guild experience reward (0 for none)
     * @param guildCoins guild coin reward (0 for none)
     * @param personal   personal reward details (nullable)
     */
    public static void sendCompletionMessage(Player player, String header,
                                             String questName,
                                             int guildExp, int guildCoins,
                                             QuestReward personal) {
        ChatFormatter.constructDivider(player, "", 45);
        ChatFormatter.sendCenteredMessage(player, header);
        ChatFormatter.sendCenteredMessage(player, ChatColor.YELLOW + questName);
        ChatFormatter.constructDivider(player, " ", 45);
        ChatFormatter.sendIndentedMessage(player, ChatColor.GREEN + "Rewards:");

        String expLabel = ChatFormatter.experienceLabel();
        String expColor = ChatFormatter.experienceColor();
        if (guildExp > 0) {
            ChatFormatter.sendIndentedMessage(player,
                    ChatColor.GREEN + "- " + expColor + guildExp + ChatColor.RESET +
                            " <glyph:experience_orb_icon> " + expLabel + ChatColor.GRAY + " (Guild)");
        }
        if (guildCoins > 0) {
            ChatFormatter.sendIndentedMessage(player,
                    ChatColor.GREEN + "- " + ChatColor.GRAY + guildCoins +
                            " <glyph:coins_icon>" + ChatColor.GRAY + " (Guild)");
        }
        if (personal != null) {
            if (personal.getXp() > 0) {
                ChatFormatter.sendIndentedMessage(player,
                        ChatColor.GREEN + "- " + expColor + personal.getXp() + ChatColor.RESET +
                                " <glyph:experience_orb_icon> " + expLabel);
            }
            if (personal.getCoins() > 0) {
                ChatFormatter.sendIndentedMessage(player,
                        ChatColor.GREEN + "- " + ChatColor.GRAY + personal.getCoins() + " <glyph:coins_icon>");
            }
            if (personal.getGems() > 0) {
                ChatFormatter.sendIndentedMessage(player,
                        ChatColor.GREEN + "- " + ChatColor.GRAY + personal.getGems() + " " + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>");
            }
            Main plugin = Main.getInstance();
            for (int id : personal.getItemIds()) {
                CustomItem tpl = plugin.getItemManager().getTemplateById(id);
                String name = tpl != null ? tpl.getBaseName() : ("Item " + id);
                ChatFormatter.sendIndentedMessage(player, ChatColor.GREEN + "- " + ChatColor.GRAY + name);
            }
            for (PlayerClass pc : personal.getUnlockClasses()) {
                String pretty = pc.name().substring(0,1) + pc.name().substring(1).toLowerCase();
                ChatFormatter.sendIndentedMessage(player, ChatColor.GREEN + "- " + ChatColor.GRAY + pretty + " Class");
            }
        }
        ChatFormatter.constructDivider(player, " ", 45);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
}

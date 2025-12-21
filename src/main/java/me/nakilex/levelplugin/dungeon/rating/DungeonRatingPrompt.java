package me.nakilex.levelplugin.dungeon.rating;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

/**
 * Conversation prompt asking the player to enter a dungeon rating.
 */
public class DungeonRatingPrompt extends StringPrompt {
    private final String dungeonKey;
    private final Player player;

    public DungeonRatingPrompt(String dungeonKey, Player player) {
        this.dungeonKey = dungeonKey;
        this.player = player;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return ChatColor.GOLD + "Enter a rating 1-5 (one decimal).";
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (input == null || !input.matches("[1-5](\\.[0-9])?")) {
            player.sendMessage(ChatColor.RED + "Please enter a number between 1 and 5.");
            return this;
        }
        double rating = Double.parseDouble(input);
        Main.getInstance().getDungeonRatingManager().addRating(dungeonKey, rating);
        Main.getInstance().getLevelManager().addXP(player, 100);

        me.nakilex.levelplugin.dungeon.DungeonManager dm = Main.getInstance().getDungeonManager();
        String displayName = dm.getDisplayName(dungeonKey);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Submitted a " + ChatColor.WHITE + rating + ChatColor.GREEN + " for " + ChatColor.WHITE + displayName + ChatColor.GREEN + " and earned a reward!");
        String expLabel = ChatFormatter.experienceLabel();
        String expColor = ChatFormatter.experienceColor();
        ChatFormatter.sendBoxedCenteredMessages(player, "§a",
                "§aThank you for your rating!",
                "§7Registered §f" + rating + "§7 for §f" + displayName,
                "§7You earned " + expColor + "+100 <glyph:experience_orb_icon> " + expLabel);
        dm.clearPendingRating(player.getUniqueId());
        return Prompt.END_OF_CONVERSATION;
    }
}

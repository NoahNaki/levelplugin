package me.nakilex.levelplugin.auctionhouse.gui;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

/**
 * Conversation prompt for entering a listing price via chat.
 */
public class PriceInputPrompt extends StringPrompt {
    private final ListingMenu menu;
    private final Player player;

    public PriceInputPrompt(ListingMenu menu, Player player) {
        this.menu = menu;
        this.player = player;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return ChatColor.GOLD + "Enter listing price (type 'cancel' to abort):";
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "Price entry cancelled.");
            menu.reopen(player, false);
            return Prompt.END_OF_CONVERSATION;
        }
        if (!input.matches("\\d+(\\.\\d+)?")) {
            player.sendMessage(ChatColor.RED + "Invalid number. Try again or type 'cancel'.");
            return this;
        }
        double price = Double.parseDouble(input);
        menu.setPrice(player, price);
        player.sendMessage(ChatColor.GREEN + "Price set to " + price);
        menu.reopen(player, true);
        return Prompt.END_OF_CONVERSATION;
    }
}

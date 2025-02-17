package me.nakilex.levelplugin.utils;

import org.bukkit.conversations.StringPrompt;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CoinInputPrompt extends StringPrompt {
    private final TradingWindow tradingWindow;
    private final Player player;

    public CoinInputPrompt(TradingWindow tradingWindow, Player player) {
        this.tradingWindow = tradingWindow;
        this.player = player;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return ChatColor.GOLD + "Please enter the number of coins you want to offer:";
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (!input.matches("\\d+")) {
            player.sendMessage(ChatColor.RED + "Invalid input! Please enter a valid number.");
            return this; // re-prompt the same message
        }
        int coins = Integer.parseInt(input);
        // Check if the player has enough coins
        if (tradingWindow.getEconomyManager().getBalance(player) < coins) {
            player.sendMessage(ChatColor.RED + "You do not have enough coins to offer that amount.");
            return Prompt.END_OF_CONVERSATION;
        }

        // Update the correct coin offer based on who the player is in this trade.
        if (tradingWindow.getPlayer().equals(player)) {
            tradingWindow.setPlayerCoinOffer(coins);
            player.sendMessage(ChatColor.GREEN + "Your coin offer has been set to: " + coins);
        } else if (tradingWindow.getOpponent().equals(player)) {
            tradingWindow.setOpponentCoinOffer(coins);
            player.sendMessage(ChatColor.GREEN + "Your coin offer has been set to: " + coins);
        }

        // Update the coin display and re-open the inventories
        tradingWindow.updateCoinOfferItems();
        tradingWindow.reopenInventories();
        return Prompt.END_OF_CONVERSATION;
    }
}

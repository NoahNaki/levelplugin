package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * Generic chat prompt for requesting a coin amount from a player.
 * A validator is used to verify the amount and an action is executed
 * once a valid amount is supplied.
 */
public class CoinInputPrompt extends StringPrompt {
    private final Player player;
    private final String promptText;
    private final IntPredicate validator;
    private final IntConsumer onAccept;

    public CoinInputPrompt(Player player, String promptText,
                           IntPredicate validator, IntConsumer onAccept) {
        this.player = player;
        this.promptText = promptText;
        this.validator = validator;
        this.onAccept = onAccept;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return promptText;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (!input.matches("\\d+")) {
            player.sendMessage(ChatColor.RED + "Invalid input! Please enter a valid number.");
            return this;
        }
        int coins = Integer.parseInt(input);
        if (!validator.test(coins)) {
            player.sendMessage(ChatColor.RED + "You do not have enough coins for that.");
            return Prompt.END_OF_CONVERSATION;
        }
        onAccept.accept(coins);
        return Prompt.END_OF_CONVERSATION;
    }
}

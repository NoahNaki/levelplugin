package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;

/**
 * Generic chat prompt for requesting a decimal number from a player.
 * A validator is used to verify the amount and an action is executed
 * once a valid number is supplied.
 */
public class DoubleInputPrompt extends NumericInputPrompt<Double> {
    private static final String DEFAULT_INVALID_DOUBLE = "Invalid input! Please enter a valid number.";
    private static final String DEFAULT_PERCENT_RANGE = "Please enter a value between 0 and 100.";

    public DoubleInputPrompt(Plugin plugin,
                             Player player,
                             String promptText,
                             String invalidInputMessage,
                             String invalidRangeMessage,
                             DoublePredicate validator,
                             DoubleConsumer onAccept) {
        super(
                plugin,
                player,
                promptText,
                ChatColor.RED + invalidInputMessage,
                ChatColor.RED + invalidRangeMessage,
                input -> Double.parseDouble(input.replace("%", "")),
                validator::test,
                onAccept::accept
        );
    }

    public static DoubleInputPrompt percentagePrompt(Plugin plugin,
                                                      Player player,
                                                      String promptText,
                                                      DoubleConsumer onAccept) {
        return new DoubleInputPrompt(
                plugin,
                player,
                promptText,
                DEFAULT_INVALID_DOUBLE,
                DEFAULT_PERCENT_RANGE,
                value -> value >= 0.0 && value <= 100.0,
                onAccept
        );
    }
}

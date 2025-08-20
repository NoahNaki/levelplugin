package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/** Utility for consistent currency messages. */
public final class CurrencyMessageUtil {
    private CurrencyMessageUtil() {}

    public enum Currency {
        COINS(ChatColor.YELLOW, "<glyph:coins_icon>", "coins"),
        GEMS(ChatColor.LIGHT_PURPLE, "<glyph:purple_orb_icon>", "gems");

        private final ChatColor amountColor;
        private final String glyph;
        private final String label;

        Currency(ChatColor amountColor, String glyph, String label) {
            this.amountColor = amountColor;
            this.glyph = glyph;
            this.label = label;
        }
    }

    public static void sendReceive(Player player, Currency currency, int amount) {
        send(player, MessageType.REWARD,
                "You received " + formatAmount(currency, amount) + ChatColor.GOLD + "!");
    }

    public static void sendLoss(Player player, Currency currency, int amount) {
        send(player, MessageType.WARNING,
                "You lost " + formatAmount(currency, amount) + ChatColor.GOLD + "!");
    }

    public static String formatAmount(Currency currency, int amount) {
        return currency.amountColor + String.valueOf(amount) + " " +
                currency.glyph + " " + ChatColor.GOLD + currency.label;
    }
}

package me.nakilex.levelplugin.serverboard;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.currency.model.XPrisonCurrency;
import me.nakilex.levelplugin.server.VaultEconomyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Per-player numbers for the idle prison scoreboard: Vault money plus X-Prison's gems, tokens, rank
 * and prestige. All the X-Prison getters used here (getBalance, getPlayerRank, getPlayerPrestige) read
 * an in-memory per-online-player cache rather than the database - verified by decompiling the plugin
 * jar (see the [[xprison-leaderboard-data]] memory, which found the opposite for the *top-N* lookups
 * the animated leaderboard uses). Safe to call every scoreboard tick.
 */
public final class PrisonScoreboardStats {
    private PrisonScoreboardStats() {
    }

    public static boolean xPrisonEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("X-Prison");
    }

    /** Vault economy balance, short-formatted, or null if no economy plugin is registered. */
    public static String money(Player player) {
        Double balance = VaultEconomyUtil.getBalance(player);
        return balance == null ? null : formatShort(balance);
    }

    /** X-Prison "gems" currency balance, using that currency's own configured format. */
    public static String gems(Player player) {
        return currencyBalance(player, "gems");
    }

    /** X-Prison "tokens" currency balance, using that currency's own configured format. */
    public static String tokens(Player player) {
        return currencyBalance(player, "tokens");
    }

    private static String currencyBalance(Player player, String currencyId) {
        if (!xPrisonEnabled()) return null;
        try {
            XPrisonCurrency currency = XPrisonAPI.getInstance().getCurrencyApi().getCurrency(currencyId);
            if (currency == null) return null;
            double balance = XPrisonAPI.getInstance().getCurrencyApi().getBalance(player, currencyId);
            return currency.format(balance);
        } catch (RuntimeException | LinkageError ex) {
            return null;
        }
    }

    /** The player's current rank prefix (e.g. "&7[A]"), or null if X-Prison ranks aren't available. */
    public static String rankPrefix(Player player) {
        if (!xPrisonEnabled()) return null;
        try {
            var rank = XPrisonAPI.getInstance().getRanksApi().getPlayerRank(player);
            return rank == null ? null : rank.getPrefix();
        } catch (RuntimeException | LinkageError ex) {
            return null;
        }
    }

    /** The player's current prestige prefix, or null if X-Prison prestiges aren't available. */
    public static String prestigePrefix(Player player) {
        if (!xPrisonEnabled()) return null;
        try {
            var prestige = XPrisonAPI.getInstance().getPrestigesApi().getPlayerPrestige(player);
            return prestige == null ? null : prestige.getPrefix();
        } catch (RuntimeException | LinkageError ex) {
            return null;
        }
    }

    /** Abbreviates a raw number as 1.2K / 3.4M / 5.6B / 7.8T, matching X-Prison's own currency style. */
    private static String formatShort(double value) {
        double abs = Math.abs(value);
        String suffix = "";
        double scaled = value;
        if (abs >= 1_000_000_000_000.0) { suffix = "T"; scaled = value / 1_000_000_000_000.0; }
        else if (abs >= 1_000_000_000.0) { suffix = "B"; scaled = value / 1_000_000_000.0; }
        else if (abs >= 1_000_000.0) { suffix = "M"; scaled = value / 1_000_000.0; }
        else if (abs >= 1_000.0) { suffix = "K"; scaled = value / 1_000.0; }
        else {
            return String.format("%,.0f", value);
        }
        return String.format("%.1f%s", scaled, suffix);
    }
}

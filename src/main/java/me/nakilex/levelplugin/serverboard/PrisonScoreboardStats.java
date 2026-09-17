package me.nakilex.levelplugin.serverboard;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.currency.model.XPrisonCurrency;
import me.nakilex.levelplugin.server.VaultEconomyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Per-player numbers for the idle prison scoreboard: Vault money (shown as Coins) plus X-Prison's
 * gems, tokens and prestige, and LevelPlugin's own rebirth count (ranks are unused - this prison
 * doesn't use X-Prison's rank-up-through-mines progression). All the X-Prison getters used here
 * (getBalance, getPlayerPrestige) read
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

    /**
     * Strips the currency's own prefix/suffix (e.g. its icon glyph) back off the formatted
     * string - {@link IdlePrisonBoard} already shows one icon per row itself, so leaving the
     * currency's own baked-in prefix in would double it up.
     */
    private static String currencyBalance(Player player, String currencyId) {
        if (!xPrisonEnabled()) return null;
        try {
            XPrisonCurrency currency = XPrisonAPI.getInstance().getCurrencyApi().getCurrency(currencyId);
            if (currency == null) return null;
            double balance = XPrisonAPI.getInstance().getCurrencyApi().getBalance(player, currencyId);
            String formatted = currency.format(balance);
            String prefix = currency.getPrefix();
            String suffix = currency.getSuffix();
            if (prefix != null && !prefix.isEmpty() && formatted.startsWith(prefix)) {
                formatted = formatted.substring(prefix.length());
            }
            if (suffix != null && !suffix.isEmpty() && formatted.endsWith(suffix)) {
                formatted = formatted.substring(0, formatted.length() - suffix.length());
            }
            return formatted;
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

    /** LevelPlugin's own infinite rebirth count layered on top of X-Prison; see XPrisonRebirthManager. */
    public static String rebirths(Player player) {
        try {
            var manager = me.nakilex.levelplugin.Main.getInstance().getXPrisonRebirthManager();
            if (manager == null) return null;
            return String.valueOf(manager.getRebirths(player.getUniqueId()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Abbreviates a raw number as 1.2K / 3.4M / 5.6B / 7.8T, matching X-Prison's own currency style. */
    static String formatShort(double value) {
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

package me.nakilex.levelplugin.serverboard;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Branded sidebar content shown when {@link me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager}
 * has nothing contextual to display (no quest, party, queue, siege, or dungeon in progress) - replicating
 * the always-on server-identity board TAB's {@code scoreboard.yml} used to show, natively.
 * <p>
 * Icon glyphs ({@code } money, {@code } coin, {@code } token, {@code } medal,
 * {@code } castle) are bitmap providers already merged into {@code minecraft:default} by the
 * "PremiumSetupTexturepack" external pack Nexo has loaded - the same codepoints TAB's config.yml used.
 * They need no font override and no new asset from us, but that also means they depend on that external
 * pack staying installed; if it's ever removed these fall back to missing-glyph boxes.
 */
public final class IdlePrisonBoard {
    public static final String TITLE = net.md_5.bungee.api.ChatColor.of("#FF7080") + "" + ChatColor.BOLD + "Premium Prison";

    private static final String ACCENT = net.md_5.bungee.api.ChatColor.of("#FF7080").toString();
    private static final String LABEL = net.md_5.bungee.api.ChatColor.of("#55C4EC").toString();
    private static final char ICON_MONEY = '';
    private static final char ICON_GEMS = '';
    private static final char ICON_TOKENS = '';
    private static final char ICON_RANK = '';
    private static final char ICON_PRESTIGE = '';

    private IdlePrisonBoard() {
    }

    public static List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "NitroSetups");
        lines.add(" ");
        lines.add(ACCENT + player.getName() + ":");
        lines.add(statLine(ICON_MONEY, "Money", PrisonScoreboardStats.money(player)));
        lines.add(statLine(ICON_GEMS, "Gems", PrisonScoreboardStats.gems(player)));
        lines.add(statLine(ICON_TOKENS, "Tokens", PrisonScoreboardStats.tokens(player)));
        lines.add(" ");
        lines.add(ACCENT + "Prison:");
        lines.add(statLine(ICON_RANK, "Rank", PrisonScoreboardStats.rankPrefix(player)));
        lines.add(statLine(ICON_PRESTIGE, "Prestige", PrisonScoreboardStats.prestigePrefix(player)));
        lines.add(" ");
        lines.add("    " + ChatColor.GRAY + "" + ChatColor.ITALIC + "www.prison.store");
        return lines;
    }

    private static String statLine(char icon, String label, String value) {
        String shown = value == null ? ChatColor.DARK_GRAY + "N/A" : ChatColor.WHITE + value;
        return " " + ChatColor.WHITE + icon + " " + LABEL + label + ": " + shown;
    }
}

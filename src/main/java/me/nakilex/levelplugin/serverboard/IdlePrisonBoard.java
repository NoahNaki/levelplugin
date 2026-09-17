package me.nakilex.levelplugin.serverboard;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Branded sidebar content shown when {@link me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager}
 * has nothing contextual to display (no quest, party, queue, siege, or dungeon in progress) - replicating
 * the always-on server-identity board TAB's {@code scoreboard.yml} used to show, natively.
 * <p>
 * Icon glyphs: Coins ({@code coins_icon}), Gems ({@code purple_orb_icon}), Tokens
 * ({@code tokens_icon}) and Rebirth ({@code crown_icon}) are our own glyphs from
 * {@code icons_pack.yml} - Nexo compiles those into
 * the {@code nexo:default} font, NOT {@code minecraft:default}, so they only render correctly when
 * the {@code font(Key.key("nexo","default"))} component style is applied explicitly (confirmed by
 * inspecting the built pack.zip: these codepoints appear in {@code assets/nexo/font/default.json},
 * not {@code assets/minecraft/font/default.json}). A bare legacy/raw character - which is what this
 * class and X-Prison's own currency `prefix:` config both did before - falls back to whatever font
 * IS active (usually {@code minecraft:default}), where these codepoints resolve to an unrelated Yi
 * Radical glyph instead of the intended icon. That's why {@code Team.setPrefix(String)} won't work
 * for this board - {@link me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager} renders these
 * lines through Paper's {@code Team.prefix(Component)} instead so the font can actually be set.
 * Prestige alone still comes from the raw external "PremiumSetupTexturepack" glyph Nexo has
 * loaded - that one genuinely is merged into {@code minecraft:default} (or is a plain Unicode
 * symbol with a built-in fallback glyph), so it needs no font override.
 */
public final class IdlePrisonBoard {
    public static final String TITLE = net.md_5.bungee.api.ChatColor.of("#FF7080") + "" + org.bukkit.ChatColor.BOLD + "Premium Prison";

    private static final Key NEXO_DEFAULT_FONT = Key.key("nexo", "default");
    private static final TextColor ACCENT = TextColor.color(0xFF7080);
    private static final TextColor LABEL = TextColor.color(0x55C4EC);

    private static final char ICON_COINS = 'ꑗ';
    private static final char ICON_GEMS = 'ꑜ';
    private static final char ICON_TOKENS = 'ꨲ'; // our own tokens_icon glyph, not the old external-pack one
    private static final char ICON_REBIRTH = 'ꑟ';
    private static final char ICON_PRESTIGE = '';

    private IdlePrisonBoard() {
    }

    public static List<Component> buildLines(Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("NitroSetups", NamedTextColor.GRAY));
        lines.add(Component.text(" "));
        lines.add(Component.text(player.getName() + ":", ACCENT));
        lines.add(statLine(nexoIcon(ICON_COINS), "Coins", PrisonScoreboardStats.money(player)));
        lines.add(statLine(nexoIcon(ICON_GEMS), "Gems", PrisonScoreboardStats.gems(player)));
        lines.add(statLine(nexoIcon(ICON_TOKENS), "Tokens", PrisonScoreboardStats.tokens(player)));
        lines.add(Component.text(" "));
        lines.add(Component.text("Prison:", ACCENT));
        lines.add(statLine(nexoIcon(ICON_REBIRTH), "Rebirth", PrisonScoreboardStats.rebirths(player)));
        lines.add(statLine(plainIcon(ICON_PRESTIGE), "Prestige", PrisonScoreboardStats.prestigePrefix(player)));
        lines.add(Component.text(" "));
        lines.add(Component.text("    ")
                .append(Component.text("www.prison.store", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)));
        return lines;
    }

    private static Component nexoIcon(char icon) {
        return Component.text(String.valueOf(icon)).font(NEXO_DEFAULT_FONT);
    }

    private static Component plainIcon(char icon) {
        return Component.text(String.valueOf(icon));
    }

    private static Component statLine(Component icon, String label, String value) {
        Component shown = value == null
                ? Component.text("N/A", NamedTextColor.DARK_GRAY)
                : Component.text(value, NamedTextColor.WHITE);
        return Component.text(" ")
                .append(icon.color(NamedTextColor.WHITE))
                .append(Component.text(" "))
                .append(Component.text(label + ": ", LABEL))
                .append(shown);
    }
}

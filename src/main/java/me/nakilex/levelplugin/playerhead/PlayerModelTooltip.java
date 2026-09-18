package me.nakilex.levelplugin.playerhead;

import me.nakilex.levelplugin.dialogdemo.market.DialogPixels;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The hover card shown for a player: their live 3D model on the left, their profile on the right.
 *
 * Layout is done in GUI pixels rather than spaces. The shader grows the model's quad well past the
 * 8px the text cursor actually advances (see {@link PlayerModelComponent#OBJECT_ADVANCE_PIXELS}), so
 * padding with spaces can only ever approximate the gutter - and at the default bust scale it lands
 * about two pixels short, which is the overlap this replaces. {@link DialogPixels#shift(int)} moves
 * the cursor by an exact number of pixels instead, so the text column always clears the model.
 */
public final class PlayerModelTooltip {
    /** Defaults chosen for the chat-hover card: a bust, unshifted, at 3x, barely breathing. */
    public static final PlayerModelComponent.Type DEFAULT_TYPE = PlayerModelComponent.Type.BUST;
    public static final int DEFAULT_OFFSET = 0;
    public static final int DEFAULT_SCALE = 3;
    public static final double DEFAULT_SPEED = 0.13;
    public static final PlayerModelComponent.Animation DEFAULT_ANIMATION = PlayerModelComponent.Animation.IDLE;

    /** Clear space in GUI pixels between the model's right edge and the start of the text column. */
    private static final int GUTTER_PIXELS = 8;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private PlayerModelTooltip() {
    }

    /** The profile card at the standard chat-hover settings. */
    public static Component create(Player target) {
        return create(target, DEFAULT_TYPE, DEFAULT_OFFSET, DEFAULT_SCALE, DEFAULT_SPEED, DEFAULT_ANIMATION);
    }

    public static Component create(Player target,
                                   PlayerModelComponent.Type type,
                                   int offset,
                                   int scale,
                                   double speed,
                                   PlayerModelComponent.Animation animation) {
        Component portrait = PlayerModelComponent.create(
                target.getUniqueId(), type, offset, scale, speed, animation);
        return card(portrait, profileLines(target), type, offset, scale);
    }

    /**
     * The same card for a subject that is not a real {@link Player} - a spoofed player, which has a
     * portrait and a few profile lines but no live server state to read.
     */
    public static Component card(Component portrait, List<Component> profileLines) {
        return card(portrait, new ArrayList<>(profileLines), DEFAULT_TYPE, DEFAULT_OFFSET, DEFAULT_SCALE);
    }

    private static Component card(Component portrait, List<Component> lines,
                                  PlayerModelComponent.Type type, int offset, int scale) {
        int columnStart = type.pixelWidth(scale) + GUTTER_PIXELS;

        // The model hangs below its own line; keep adding blank rows until the card is tall enough.
        int rowsCovered = (int) Math.ceil((type.pixelHeight(scale) + offset) / (double) DialogPixels.LINE_HEIGHT);
        while (lines.size() < rowsCovered) {
            lines.add(Component.empty());
        }

        Component card = Component.empty()
                .append(portrait)
                .append(DialogPixels.shift(columnStart - PlayerModelComponent.OBJECT_ADVANCE_PIXELS))
                .append(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            card = card.append(Component.newline())
                    .append(DialogPixels.shift(columnStart))
                    .append(lines.get(i));
        }
        return card;
    }

    /** The right-hand column: who they are and where they are in the game. */
    private static List<Component> profileLines(Player target) {
        List<Component> lines = new ArrayList<>();
        lines.add(target.displayName().decoration(TextDecoration.BOLD, true));
        lines.add(legacy(PlayerRankUtil.rankLabel(target)));

        LevelManager levels = LevelManager.getInstance();
        if (levels != null) {
            lines.add(label("Level", Component.text(levels.getLevel(target), NamedTextColor.WHITE)));
        }

        StatsManager stats = StatsManager.getInstance();
        StatsManager.PlayerStats playerStats = stats == null ? null : stats.getPlayerStats(target.getUniqueId());
        PlayerClass playerClass = playerStats == null ? null : playerStats.playerClass;
        if (playerClass != null && playerClass != PlayerClass.VILLAGER) {
            lines.add(label("Class", Component.text(playerClass.getDisplayName(), NamedTextColor.AQUA)));
        }

        Guild guild = GuildManager.getInstance() == null ? null : GuildManager.getInstance().getGuild(target.getUniqueId());
        if (guild != null) {
            lines.add(label("Guild", Component.text(guild.getName(), NamedTextColor.GREEN)));
        }

        lines.add(Component.text("❤ ", NamedTextColor.RED)
                .append(Component.text((int) Math.ceil(target.getHealth()), NamedTextColor.WHITE))
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text((int) Math.ceil(stats == null ? target.getHealth() : stats.getMaxHealth(target)),
                        NamedTextColor.GRAY)));

        if (playerStats != null) {
            lines.add(Component.text("✦ ", NamedTextColor.BLUE)
                    .append(Component.text(playerStats.getCurrentMana(), NamedTextColor.WHITE))
                    .append(Component.text("/", NamedTextColor.DARK_GRAY))
                    .append(Component.text(playerStats.getMaxMana(), NamedTextColor.GRAY)));
        }
        return lines;
    }

    private static Component label(String label, Component value) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(value);
    }

    private static Component legacy(String text) {
        return LEGACY.deserialize(ChatColor.translateAlternateColorCodes('&', text));
    }
}

package me.nakilex.playerspoofer;

import me.nakilex.levelplugin.playerhead.PlayerHeadRenderer;
import me.nakilex.levelplugin.playerhead.PlayerModelComponent;
import me.nakilex.levelplugin.playerhead.PlayerModelTooltip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dresses a fake player's chat line up like a real player's: their face glyph, then a rank prefix,
 * then the name.
 *
 * <p>Ranks are display-only. Each bot is assigned a LuckPerms <em>group</em> and renders that
 * group's configured prefix, but no LuckPerms user record is ever written. That is deliberate:
 * bots reuse the UUIDs of real, verified Mojang accounts, so persisting a rank against those
 * UUIDs would hand a free rank to the real owner if they ever joined.</p>
 *
 * <p>The assignment is derived from the bot's UUID, so a given bot keeps the same rank for its
 * whole life and across restarts without anything being stored.</p>
 */
final class FakeChatAppearance {

    /** Matches the {@code &#RRGGBB} hex colours LuckPerms prefixes on this server use. */
    private static final Pattern LEGACY_HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("&([0-9A-Fa-fK-Ok-oRr])");

    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset"));

    /** Default spread: mostly members, with a thin tail of donor ranks so chat looks real. */
    private static final Map<String, Integer> DEFAULT_WEIGHTS = new LinkedHashMap<>(Map.of(
            "default", 75, "vip", 8, "vip+", 5, "mvp", 4,
            "elite", 3, "titan", 2, "master", 2, "legend", 1));

    private final PlayerSpooferPlugin plugin;
    private final Map<UUID, String> groupByBot = new ConcurrentHashMap<>();
    private final Map<String, Component> prefixByGroup = new ConcurrentHashMap<>();

    FakeChatAppearance(PlayerSpooferPlugin plugin) {
        this.plugin = plugin;
    }

    /** The full chat line for a bot: face, rank prefix, name, then the message. */
    Component chatLine(FakePlayer bot, Component message) {
        // The hover sits on the face + prefix + name together, matching how a real player's line
        // is built in ChatUtil - that whole block is what people actually aim at.
        Component speaker = Component.text()
                .append(head(bot))
                .append(Component.text(" "))
                .append(prefix(bot))
                .append(Component.text(bot.name(), NamedTextColor.WHITE))
                .hoverEvent(HoverEvent.showText(tooltip(bot)))
                .build();
        return Component.text()
                .append(speaker)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(message)
                .build();
    }

    /**
     * The bot's name as it appears in the tab list: rank prefix then name, so a spoofed player
     * carries the same rank glyph a real player does.
     */
    Component tabName(FakePlayer bot) {
        return Component.text()
                .append(prefix(bot))
                .append(Component.text(bot.name(), NamedTextColor.WHITE))
                .build();
    }

    /**
     * The hover card, built from the same layout real players get. Only the lines the spoofer
     * actually knows are shown - a bot has no level, class, guild or health to report, so claiming
     * any would be inventing server state that does not exist.
     */
    private Component tooltip(FakePlayer bot) {
        Component portrait = PlayerModelComponent.create(
                bot.uuid(), bot.name(), bot.skinTexture(), bot.skinSignature(),
                PlayerModelTooltip.DEFAULT_TYPE, PlayerModelTooltip.DEFAULT_OFFSET,
                PlayerModelTooltip.DEFAULT_SCALE, PlayerModelTooltip.DEFAULT_SPEED,
                PlayerModelTooltip.DEFAULT_ANIMATION);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(bot.name(), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
        Component rank = prefix(bot);
        if (!Component.empty().equals(rank)) {
            lines.add(rank);
        }
        lines.add(Component.text("Ping: ", NamedTextColor.GRAY)
                .append(Component.text(bot.ping() + "ms", NamedTextColor.WHITE)));
        return PlayerModelTooltip.card(portrait, lines);
    }

    private Component head(FakePlayer bot) {
        return PlayerHeadRenderer.getHead(plugin.host(), bot.uuid(), bot.skinTexture());
    }

    /** The bot's rank prefix, already rendered - empty when the group has no prefix. */
    private Component prefix(FakePlayer bot) {
        String group = groupFor(bot);
        return prefixByGroup.computeIfAbsent(group, this::renderGroupPrefix);
    }

    /**
     * The group this bot belongs to. Chosen once from the configured weights using the bot's UUID,
     * so it is stable for that bot without being stored anywhere.
     */
    String groupFor(FakePlayer bot) {
        return groupByBot.computeIfAbsent(bot.uuid(), id -> pickGroup(id, weights()));
    }

    private static String pickGroup(UUID id, Map<String, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return "default";
        }
        // Math.floorMod keeps this positive for UUIDs whose hash is negative.
        int roll = Math.floorMod(id.hashCode(), total);
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return "default";
    }

    private Map<String, Integer> weights() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("chat.display.rank-weights");
        if (section == null) {
            return DEFAULT_WEIGHTS;
        }
        Map<String, Integer> configured = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            int weight = section.getInt(key, 0);
            if (weight > 0) {
                configured.put(key.toLowerCase(Locale.ROOT), weight);
            }
        }
        return configured.isEmpty() ? DEFAULT_WEIGHTS : configured;
    }

    private Component renderGroupPrefix(String group) {
        String raw = LuckPermsGroupPrefix.of(group);
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        try {
            return miniMessage().deserialize(toMiniMessage(raw));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not render the '" + group + "' rank prefix for fake chat: "
                    + ex.getMessage());
            return Component.empty();
        }
    }

    /**
     * MiniMessage with Nexo's {@code <glyph:...>} resolver attached when Nexo is present, since
     * several rank prefixes on this server are glyphs rather than text.
     */
    private MiniMessage miniMessage() {
        List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(TagResolver.standard());
        TagResolver glyphs = nexoGlyphResolver();
        if (glyphs != null) {
            resolvers.add(glyphs);
        }
        return MiniMessage.builder().tags(TagResolver.resolver(resolvers)).build();
    }

    private TagResolver nexoGlyphResolver() {
        try {
            Class<?> tag = Class.forName("com.nexomc.nexo.glyphs.GlyphTag");
            Object instance = tag.getField("INSTANCE").get(null);
            return (TagResolver) tag.getMethod("getRESOLVER").invoke(instance);
        } catch (Throwable ignored) {
            // Nexo missing or its internals moved: text prefixes still render, glyphs drop out.
            return null;
        }
    }

    /** Converts the {@code &#RRGGBB} / {@code &l} codes LuckPerms stores into MiniMessage tags. */
    private static String toMiniMessage(String raw) {
        Matcher hex = LEGACY_HEX.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (hex.find()) {
            hex.appendReplacement(out, "<#" + hex.group(1) + ">");
        }
        hex.appendTail(out);

        Matcher code = LEGACY_CODE.matcher(out.toString());
        StringBuilder result = new StringBuilder();
        while (code.find()) {
            String tag = LEGACY_TAGS.get(Character.toLowerCase(code.group(1).charAt(0)));
            code.appendReplacement(result, tag == null ? "" : "<" + tag + ">");
        }
        code.appendTail(result);
        return result.toString();
    }
}

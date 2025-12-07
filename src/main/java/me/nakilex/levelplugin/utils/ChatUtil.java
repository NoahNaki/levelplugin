package me.nakilex.levelplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility methods for player chat messages. */
public final class ChatUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-z_]+):", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> EMOJI_GLYPHS = Map.ofEntries(
            Map.entry("begging", glyphTag("begging_emote")),
            Map.entry("begging_emote", glyphTag("begging_emote")),
            Map.entry("clown", glyphTag("clown_emote")),
            Map.entry("clown_emote", glyphTag("clown_emote")),
            Map.entry("crying", glyphTag("crying_emote")),
            Map.entry("crying_emote", glyphTag("crying_emote")),
            Map.entry("eyes", glyphTag("eyes_emote")),
            Map.entry("eyes_emote", glyphTag("eyes_emote")),
            Map.entry("fire", glyphTag("fire_emote")),
            Map.entry("fire_emote", glyphTag("fire_emote")),
            Map.entry("grimacing", glyphTag("grimacing_emote")),
            Map.entry("grimacing_emote", glyphTag("grimacing_emote")),
            Map.entry("happy", glyphTag("happy_emote")),
            Map.entry("happy_emote", glyphTag("happy_emote")),
            Map.entry("heart", glyphTag("heart_emote")),
            Map.entry("heart_emote", glyphTag("heart_emote")),
            Map.entry("hearteyes", glyphTag("hearteyes_emote")),
            Map.entry("hearteyes_emote", glyphTag("hearteyes_emote")),
            Map.entry("joy", glyphTag("joy_emote")),
            Map.entry("joy_emote", glyphTag("joy_emote")),
            Map.entry("love", glyphTag("love_emote")),
            Map.entry("love_emote", glyphTag("love_emote")),
            Map.entry("rage", glyphTag("rage_emote")),
            Map.entry("rage_emote", glyphTag("rage_emote")),
            Map.entry("rainbow", glyphTag("rainbow_emote")),
            Map.entry("rainbow_emote", glyphTag("rainbow_emote")),
            Map.entry("sad", glyphTag("sad_emote")),
            Map.entry("sad_emote", glyphTag("sad_emote")),
            Map.entry("skull", glyphTag("skull_emote")),
            Map.entry("skull_emote", glyphTag("skull_emote")),
            Map.entry("smile", glyphTag("smiling_emote")),
            Map.entry("smiling", glyphTag("smiling_emote")),
            Map.entry("smiling_emote", glyphTag("smiling_emote")),
            Map.entry("sunglasses", glyphTag("sunglasses_emote")),
            Map.entry("sunglasses_emote", glyphTag("sunglasses_emote")),
            Map.entry("sweat", glyphTag("sweat_emote")),
            Map.entry("sweat_emote", glyphTag("sweat_emote")),
            Map.entry("thumbdown", glyphTag("thumbdown_emote")),
            Map.entry("thumbdown_emote", glyphTag("thumbdown_emote")),
            Map.entry("thumbup", glyphTag("thumbup_emote")),
            Map.entry("thumbup_emote", glyphTag("thumbup_emote")),
            Map.entry("tongue", glyphTag("tongue_emote")),
            Map.entry("tongue_emote", glyphTag("tongue_emote")),
            Map.entry("upsidedown", glyphTag("upsidedown_emote")),
            Map.entry("upsidedown_emote", glyphTag("upsidedown_emote")),
            Map.entry("wink", glyphTag("wink_emote")),
            Map.entry("wink_emote", glyphTag("wink_emote"))
    );

    private static String glyphTag(String emoteName) {
        return "<glyph:" + emoteName + ">";
    }

    private ChatUtil() {
    }

    /**
     * Build a chat message component, replacing [item] with the sender's held item if present.
     */
    public static Component buildMessage(Player player, String message) {
        message = applyEmojis(message);
        if (!message.toLowerCase().contains("[item]")) {
            return Component.text()
                    .append(player.displayName())
                    .append(Component.text(": " + message))
                    .build();
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item to use [item].");
            String stripped = message.replaceAll("(?i)\\[item\\]", "");
            return Component.text()
                    .append(player.displayName())
                    .append(Component.text(": " + stripped))
                    .build();
        }

        Component itemComponent = stack.displayName().hoverEvent(stack.asHoverEvent());
        String placeholder = "<itemlink>";
        String replaced = message.replaceAll("(?i)\\[item\\]", placeholder);
        Component msg = LEGACY.deserialize(replaced);
        Component combined = msg.replaceText(TextReplacementConfig.builder()
                .match(placeholder)
                .replacement(itemComponent)
                .build());

        return Component.text()
                .append(player.displayName())
                .append(Component.text(": "))
                .append(combined)
                .build();
    }

    /**
     * Replace :emoji: shortcodes with their configured glyph tags for the font provider.
     */
    public static String applyEmojis(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = EMOJI_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String glyph = EMOJI_GLYPHS.get(key);
            if (glyph != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(glyph));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

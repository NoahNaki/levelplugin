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
            Map.entry("begging", "꒝"),
            Map.entry("begging_emote", "꒝"),
            Map.entry("clown", "꒞"),
            Map.entry("clown_emote", "꒞"),
            Map.entry("crying", "꒟"),
            Map.entry("crying_emote", "꒟"),
            Map.entry("eyes", "꒠"),
            Map.entry("eyes_emote", "꒠"),
            Map.entry("fire", "꒡"),
            Map.entry("fire_emote", "꒡"),
            Map.entry("grimacing", "꒢"),
            Map.entry("grimacing_emote", "꒢"),
            Map.entry("happy", "꒣"),
            Map.entry("happy_emote", "꒣"),
            Map.entry("heart", "꒤"),
            Map.entry("heart_emote", "꒤"),
            Map.entry("hearteyes", "꒥"),
            Map.entry("hearteyes_emote", "꒥"),
            Map.entry("joy", "꒦"),
            Map.entry("joy_emote", "꒦"),
            Map.entry("love", "꒧"),
            Map.entry("love_emote", "꒧"),
            Map.entry("rage", "꒨"),
            Map.entry("rage_emote", "꒨"),
            Map.entry("rainbow", "꒩"),
            Map.entry("rainbow_emote", "꒩"),
            Map.entry("sad", "꒪"),
            Map.entry("sad_emote", "꒪"),
            Map.entry("skull", "꒫"),
            Map.entry("skull_emote", "꒫"),
            Map.entry("smiling", "꒬"),
            Map.entry("smiling_emote", "꒬"),
            Map.entry("sunglasses", "꒭"),
            Map.entry("sunglasses_emote", "꒭"),
            Map.entry("sweat", "꒮"),
            Map.entry("sweat_emote", "꒮"),
            Map.entry("thumbdown", "꒯"),
            Map.entry("thumbdown_emote", "꒯"),
            Map.entry("thumbup", "꒰"),
            Map.entry("thumbup_emote", "꒰"),
            Map.entry("tongue", "꒱"),
            Map.entry("tongue_emote", "꒱"),
            Map.entry("upsidedown", "꒲"),
            Map.entry("upsidedown_emote", "꒲"),
            Map.entry("wink", "꒳"),
            Map.entry("wink_emote", "꒳")
    );

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
     * Replace :emoji: shortcodes with their configured glyph characters.
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

package me.nakilex.levelplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Utility methods for player chat messages. */
public final class ChatUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ChatUtil() {
    }

    /**
     * Build a chat message component, replacing [item] with the sender's held item if present.
     */
    public static Component buildMessage(Player player, String message) {
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
}

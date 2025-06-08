package me.nakilex.levelplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

public class ItemChatListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        if (!msg.toLowerCase().contains("[item]")) return;

        Player player = event.getPlayer();
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item to use [item].");
            return;
        }

        event.setCancelled(true);

        Component itemComponent = Bukkit.getItemFactory().displayName(stack);
        String[] parts = msg.split("(?i)\\[item\\]", -1);
        Component combined = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                combined = combined.append(LEGACY.deserialize(parts[i]));
            }
            if (i < parts.length - 1) {
                combined = combined.append(itemComponent);
            }
        }

        Component finalMsg = Component.text()
                .append(player.displayName())
                .append(Component.text(": "))
                .append(combined)
                .build();

        for (Player target : event.getRecipients()) {
            target.sendMessage(finalMsg);
        }
    }
}

package me.nakilex.levelplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

public class ItemChatListener implements Listener {

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

        Component itemComponent = stack.displayName().hoverEvent(stack.asHoverEvent());

        String placeholder = "<itemlink>";
        String replaced = msg.replaceAll("(?i)\\[item\\]", placeholder);
        Component message = ComponentUtil.LEGACY.deserialize(replaced);
        Component combined = message.replaceText(
                TextReplacementConfig.builder()
                        .match(placeholder)
                        .replacement(itemComponent)
                        .build()
        );

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

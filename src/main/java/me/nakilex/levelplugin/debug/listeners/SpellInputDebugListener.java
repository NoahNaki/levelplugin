package me.nakilex.levelplugin.debug.listeners;

import me.nakilex.levelplugin.debug.SpellInputDebugItem;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class SpellInputDebugListener implements Listener {
    private final SpellInputDisplayManager displayManager = SpellInputDisplayManager.getInstance();

    @EventHandler
    public void onSpellInput(SpellInputEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!SpellInputDebugItem.isDebugStick(held)) {
            return;
        }
        String combo = displayManager.getComboSequence(player);
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.AQUA).append("Spell Input")
                .append(ChatColor.GRAY).append(": ")
                .append(ChatColor.WHITE).append(event.getInputType().name());
        if (event.getInputSequence() != null && !event.getInputSequence().isBlank()) {
            message.append(ChatColor.GRAY).append(" (")
                    .append(ChatColor.WHITE).append(event.getInputSequence())
                    .append(ChatColor.GRAY).append(")");
        }
        if (!combo.isBlank()) {
            message.append(ChatColor.DARK_GRAY).append(" | ")
                    .append(ChatColor.GRAY).append("Combo ")
                    .append(ChatColor.WHITE).append(combo);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, message.toString());
    }
}

package me.nakilex.levelplugin.booster;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

/** Listener that activates boosters when the corresponding item is used. */
public class BoosterItemListener implements Listener {

    private final GlobalBoosterManager boosterManager;

    public BoosterItemListener(GlobalBoosterManager boosterManager) {
        this.boosterManager = boosterManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack stack = event.getItem();
        BoosterType type = BoosterItemUtil.getBoosterType(stack);
        if (type == null) return;

        event.setCancelled(true);

        boolean activated = boosterManager.activateBooster(type, Duration.ofHours(1), event.getPlayer());
        if (!activated) {
            return;
        }

        removeOne(event.getPlayer(), stack);
        event.getPlayer().sendMessage(ChatMessageUtil.format(MessageType.SUCCESS,
                "Activated a serverwide " + (type == BoosterType.COIN ? "coin" : "combat XP") + " booster!"));
    }

    private void removeOne(org.bukkit.entity.Player player, ItemStack stack) {
        if (stack == null) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.equals(stack)) {
            int newAmount = stack.getAmount() - 1;
            stack.setAmount(Math.max(0, newAmount));
            if (newAmount <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInMainHand(stack);
            }
        }
    }
}

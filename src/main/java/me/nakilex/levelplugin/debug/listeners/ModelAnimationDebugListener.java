package me.nakilex.levelplugin.debug.listeners;

import me.nakilex.levelplugin.debug.ModelAnimationDebugItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ModelAnimationDebugListener implements Listener {

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ModelAnimationDebugItem.isDebugItem(held)) {
            return;
        }

        List<String> modelIds = ModelAnimationDebugItem.getModelIds(held);
        if (modelIds.isEmpty()) {
            return;
        }

        int currentIndex = ModelAnimationDebugItem.getCurrentIndex(held);
        int nextIndex = (currentIndex + 1) % modelIds.size();
        String nextModel = modelIds.get(nextIndex);
        ItemUtil.applyNexoModel(held, nextModel);
        ModelAnimationDebugItem.setCurrentIndex(held, nextIndex);
    }
}


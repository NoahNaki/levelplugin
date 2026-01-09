package me.nakilex.levelplugin.hud.input;

import me.nakilex.levelplugin.spells.input.SpellClickInput;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HudInputListener implements Listener {
    private final SpellInputDisplayManager displayManager = SpellInputDisplayManager.getInstance();

    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationEvent.AnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        displayManager.recordClick(player, SpellClickInput.LEFT);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            displayManager.recordClick(event.getPlayer(), SpellClickInput.RIGHT);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        displayManager.recordClick(event.getPlayer(), SpellClickInput.RIGHT);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        displayManager.clear(event.getPlayer());
    }
}

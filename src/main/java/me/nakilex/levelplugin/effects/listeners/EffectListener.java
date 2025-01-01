//package me.nakilex.levelplugin.effects.listeners;
//
//import me.nakilex.levelplugin.effects.managers.EffectManager;
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;
//import org.bukkit.event.player.PlayerInteractEvent;
//
//public class EffectListener implements Listener {
//
//    private final EffectManager effectManager;
//
//    // Constructor to inject the EffectManager
//    public EffectListener(EffectManager effectManager) {
//        this.effectManager = effectManager;
//    }
//
//    @EventHandler
//    public void onPlayerInteract(PlayerInteractEvent event) {
//        Player player = event.getPlayer();
//
//        // Trigger the currently active effect
//        if (effectManager.hasActiveEffect(player)) {
//            player.sendMessage("You already have an active effect!");
//            return;
//        }
//
//        player.sendMessage("Left click detected. Trigger an effect using /effect <type>.");
//    }
//}

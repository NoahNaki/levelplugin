// EffectListener.java
package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.effects.SwordCircleEffect;
import me.nakilex.levelplugin.tasks.SwordCircleTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EffectListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Create the SwordCircleEffect
        SwordCircleEffect effect = new SwordCircleEffect();
        effect.spawnSwords(player.getLocation());

        // Start the SwordCircleTask to animate the swords
        new SwordCircleTask(effect.getArmorStands(), player)
            .runTaskTimer(JavaPlugin.getProvidingPlugin(getClass()), 0L, 1L);

        player.sendMessage("You triggered the Sword Circle Effect!");
    }
}

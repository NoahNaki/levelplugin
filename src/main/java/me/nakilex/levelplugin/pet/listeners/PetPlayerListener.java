package me.nakilex.levelplugin.pet.listeners;

import me.nakilex.levelplugin.pet.PetManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PetPlayerListener implements Listener {
    private final PetManager petManager;

    public PetPlayerListener(PetManager petManager) {
        this.petManager = petManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        petManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        petManager.handlePlayerQuit(event.getPlayer());
    }
}

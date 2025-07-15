package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.BuildingUpgradeGUI;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Opens the building upgrade GUI when players interact with hologram stands. */
public class BuildingHologramListener implements Listener {
    private final BuildingUpgradeGUI gui;

    public BuildingHologramListener(BuildingUpgradeGUI gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        var entity = event.getRightClicked();
        if (entity instanceof ArmorStand || entity instanceof TextDisplay) {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("building_hologram:")) {
                    String building = tag.substring("building_hologram:".length());
                    event.setCancelled(true);
                    gui.open(event.getPlayer(), building);
                    return;
                }
            }
        }
    }
}

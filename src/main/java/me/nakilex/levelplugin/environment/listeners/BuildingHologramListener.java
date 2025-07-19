package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.BuildingUpgradeGUI;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/** Opens the building upgrade GUI when players interact with hologram displays. */
public class BuildingHologramListener implements Listener {
    private final BuildingUpgradeGUI gui;

    public BuildingHologramListener(BuildingUpgradeGUI gui) {
        this.gui = gui;
    }

    private void handleInteract(org.bukkit.entity.Player player, org.bukkit.entity.Entity entity, Runnable cancelAction) {
        if (entity instanceof ArmorStand || entity instanceof TextDisplay || entity instanceof Interaction) {
            for (String tag : entity.getScoreboardTags()) {
                if (tag.startsWith("building_hologram:")) {
                    String building = tag.substring("building_hologram:".length());
                    cancelAction.run();
                    gui.open(player, building);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }
}

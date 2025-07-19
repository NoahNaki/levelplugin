package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.BuildingUpgradeGUI;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Opens the building upgrade GUI when players interact with hologram displays. */
public class BuildingHologramListener implements Listener {
    private final BuildingUpgradeGUI gui;

    public BuildingHologramListener(BuildingUpgradeGUI gui) {
        this.gui = gui;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        var entity = event.getRightClicked();
        Main.getPlugin().getLogger().info("[DBG] BuildingHologramListener triggered by "
                + event.getPlayer().getName() + " on " + entity.getType());
        if (entity instanceof ArmorStand || entity instanceof TextDisplay || entity instanceof Interaction) {
            for (String tag : entity.getScoreboardTags()) {
                Main.getPlugin().getLogger().info("[DBG] checking tag " + tag);
                if (tag.startsWith("building_hologram:")) {
                    String building = tag.substring("building_hologram:".length());
                    Main.getPlugin().getLogger().info("[DBG] opening building GUI for " + building);
                    event.setCancelled(true);
                    gui.open(event.getPlayer(), building);
                    return;
                }
            }
        }
        Main.getPlugin().getLogger().info("[DBG] entity was not a building hologram");
    }
}

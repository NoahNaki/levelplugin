package me.nakilex.levelplugin.screen;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Player;

/**
 * Handles simple text display spawning for menus.
 */
public class TextDisplayManager extends DisplayManager<TextDisplay> {

    /**
     * Spawn a text display for the player at the given location.
     *
     * @return the spawned TextDisplay instance
     */
    public TextDisplay show(Player player, Location location, Component text) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.text(text);
        track(player, display);
        return display;
    }
}
